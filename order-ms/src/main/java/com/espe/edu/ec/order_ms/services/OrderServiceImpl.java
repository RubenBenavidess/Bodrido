package com.espe.edu.ec.order_ms.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.espe.edu.ec.order_ms.dtos.AssignDriverRequest;
import com.espe.edu.ec.order_ms.dtos.DeliveryOrderPatchRequest;
import com.espe.edu.ec.order_ms.dtos.OrderPatchRequest;
import com.espe.edu.ec.order_ms.dtos.OrderRequest;
import com.espe.edu.ec.order_ms.dtos.OrderResponse;
import com.espe.edu.ec.order_ms.event_producers.OrderEventProducer;
import com.espe.edu.ec.order_ms.mappers.OrderMapper;
import com.espe.edu.ec.order_ms.model_enums.OrderStatus;
import com.espe.edu.ec.order_ms.model_enums.SagaStep;
import com.espe.edu.ec.order_ms.model_enums.VehicleType;
import com.espe.edu.ec.order_ms.models.Address;
import com.espe.edu.ec.order_ms.models.Order;
import com.espe.edu.ec.order_ms.models.Tariff;
import com.espe.edu.ec.order_ms.repositories.OrderRepository;
import com.espe.edu.ec.order_ms.repositories.TariffRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final TariffRepository tariffRepository;

    @Autowired
    private final OrderEventProducer orderEventProducer;

    @Override
    @Transactional
    public OrderResponse createOrder(OrderRequest orderRequest) {

        log.info("Iniciando creación de pedido para cliente: {}", orderRequest.getCustomerId());

        Order order = OrderMapper.orderRequestToEntity(orderRequest);
        calculateOrderValues(order, orderRequest.getVehicleType());

        Order newOrder = orderRepository.save(order);

        // Iniciar saga - registrar sagaStartedAt
        newOrder.setSagaStartedAt(LocalDateTime.now());
        newOrder = orderRepository.save(newOrder);

        orderEventProducer.publishOrderCreatedEvent(newOrder);

        return OrderMapper.entityToOrderResponse(newOrder);

    }

    @Override
    public OrderResponse getOrder(UUID id) {

        Order foundOrder = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado: " + id));
        return OrderMapper.entityToOrderResponse(foundOrder);

    }

    @Override
    public List<OrderResponse> getOrders() {
        return orderRepository.findAll()
                .stream()
                .map(OrderMapper::entityToOrderResponse)
                .toList();
    }

    @Override
    public boolean orderExists(UUID id) {
        return getOrder(id) == null ? false : true;
    }

    @Override
    @Transactional
    public OrderResponse patchOrder(UUID id, OrderPatchRequest orderPatchRequest) {

        Order foundOrder = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado: " + id));

        if (!OrderStatus.CREATED.equals(foundOrder.getStatus()))
            throw new IllegalStateException("Solo se pueden editar pedidos en estado CREATED");

        if (orderPatchRequest instanceof DeliveryOrderPatchRequest deliveryOrderPatchRequest) {

            foundOrder.getDeliveryAddress().setInstructions(orderPatchRequest.getInstructions());

            if (deliveryOrderPatchRequest.getNewCoordinates() != null)
                foundOrder.getDeliveryAddress().setCoordinates(deliveryOrderPatchRequest.getNewCoordinates());
        }

        Order updatedOrder = orderRepository.save(foundOrder);
        orderEventProducer.publishOrderPatchedEvent(updatedOrder);

        return OrderMapper.entityToOrderResponse(updatedOrder);

    }

    @Override
    @Transactional
    public void cancelOrder(UUID id) {

        Order foundOrder = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado: " + id));

        // Validar que la orden está en un estado cancelable
        if (!canCancelOrder(foundOrder.getStatus())) {
            throw new IllegalStateException(
                    "Una orden solo puede ser cancelada si está en estado: " +
                            "PENDING, CREATED, ASSIGNMENT_PENDING, ASSIGNED, PICKED_UP o IN_ROUTE. " +
                            "Estado actual: " + foundOrder.getStatus());
        }

        // Determinar si hay recursos asignados en FleetService que necesiten liberarse
        boolean hasAssignedResources = hasFleetResources(foundOrder);

        if (hasAssignedResources) {
            // === SAGA: Necesitamos confirmar con FleetService antes de cancelar ===
            log.info("► [CANCELLATION-SAGA] Orden {} tiene recursos asignados. Iniciando saga de cancelación.", id);

            foundOrder.setPreviousStatus(foundOrder.getStatus());
            foundOrder.setStatus(OrderStatus.CANCELLATION_PENDING);
            foundOrder.setSagaStep(SagaStep.WAITING_CANCELLATION_VERIFICATION);
            foundOrder.setCancellationSagaStartedAt(LocalDateTime.now());
            Order savedOrder = orderRepository.save(foundOrder);

            try {
                orderEventProducer.publishCancellationValidationEvent(savedOrder);
            } catch (Exception e) {
                log.error("✗ Error publicando evento de cancelación para fleet-ms. Revertiendo estado. orderId={}", id,
                        e);
                // Revertir al estado anterior si falla la publicación
                foundOrder.setStatus(foundOrder.getPreviousStatus());
                foundOrder.setPreviousStatus(null);
                foundOrder.setSagaStep(null);
                foundOrder.setCancellationSagaStartedAt(null);
                orderRepository.save(foundOrder);
                throw new RuntimeException("Error al solicitar liberación de recursos para cancelación", e);
            }
        } else {
            // === Sin recursos asignados: cancelación directa ===
            log.info("► [CANCELLATION] Orden {} sin recursos asignados. Cancelación directa.", id);
            foundOrder.setStatus(OrderStatus.CANCELLED);
            Order cancelledOrder = orderRepository.save(foundOrder);

            try {
                orderEventProducer.publishOrderCancelledEvent(cancelledOrder);
            } catch (Exception e) {
                log.error("Error publicando evento de cancelación para orderId={}", id, e);
                throw new RuntimeException("Error al publicar evento de cancelación", e);
            }
        }
    }

    /**
     * Determina si una orden tiene recursos asignados en FleetService
     * (conductor y/o vehículo) que deben ser liberados antes de cancelar.
     */
    private boolean hasFleetResources(Order order) {
        return order.getDriverId() != null && order.getVehicleId() != null
                && (OrderStatus.ASSIGNED.equals(order.getStatus()) ||
                        OrderStatus.PICKED_UP.equals(order.getStatus()) ||
                        OrderStatus.IN_ROUTE.equals(order.getStatus()));
    }

    /**
     * Valida si una orden puede ser cancelada según su estado actual
     */
    private boolean canCancelOrder(OrderStatus status) {
        return OrderStatus.PENDING.equals(status) ||
                OrderStatus.CREATED.equals(status) ||
                OrderStatus.ASSIGNMENT_PENDING.equals(status) ||
                OrderStatus.ASSIGNED.equals(status) ||
                OrderStatus.PICKED_UP.equals(status) ||
                OrderStatus.IN_ROUTE.equals(status);
    }

    @Override
    @Transactional
    public OrderResponse pickupOrder(UUID id) {

        Order foundOrder = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado: " + id));

        // Pickup solo es posible si la orden está en ASSIGNED (recursos validados por
        // FleetService)
        if (!OrderStatus.ASSIGNED.equals(foundOrder.getStatus())) {
            throw new IllegalStateException(
                    "La orden debe estar en estado ASSIGNED para ser recogida. " +
                            "Estado actual: " + foundOrder.getStatus());
        }

        // Verificar que tiene conductor y vehículo asignados
        if (foundOrder.getDriverId() == null || foundOrder.getVehicleId() == null) {
            throw new IllegalStateException(
                    "La orden debe tener conductor y vehículo asignados para ser recogida.");
        }

        // === SAGA: Validar con FleetService que los recursos siguen disponibles ===
        log.info("► [PICKUP-SAGA] Iniciando validación de pickup para orden: {}", id);

        foundOrder.setStatus(OrderStatus.PICKUP_PENDING);
        foundOrder.setSagaStep(SagaStep.WAITING_PICKUP_VERIFICATION);
        foundOrder.setPickupSagaStartedAt(LocalDateTime.now());
        Order savedOrder = orderRepository.save(foundOrder);

        try {
            orderEventProducer.publishPickupValidationEvent(savedOrder);
        } catch (Exception e) {
            log.error("✗ Error publicando evento de pickup para fleet-ms. Revertiendo estado. orderId={}", id, e);
            foundOrder.setStatus(OrderStatus.ASSIGNED);
            foundOrder.setSagaStep(SagaStep.ASSIGNMENT_VERIFIED);
            foundOrder.setPickupSagaStartedAt(null);
            orderRepository.save(foundOrder);
            throw new RuntimeException("Error al solicitar validación de pickup", e);
        }

        return OrderMapper.entityToOrderResponse(savedOrder);
    }

    @Override
    public List<OrderResponse> getOrdersByCustomer(UUID customerId) {
        return orderRepository.findByCustomerId(customerId)
                .stream()
                .map(OrderMapper::entityToOrderResponse)
                .toList();
    }

    @Override
    @Transactional
    public OrderResponse assignDriverAndVehicle(UUID orderId, AssignDriverRequest request) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado: " + orderId));

        // Validar que la orden está en estado CREATED (solo en este estado se puede
        // asignar)
        if (!OrderStatus.CREATED.equals(order.getStatus())) {
            throw new IllegalStateException(
                    "No se puede asignar recursos. La orden debe estar en estado CREATED, " +
                            "estado actual: " + order.getStatus());
        }

        // Guardar temporalmente los IDs que queremos validar
        order.setDriverId(request.getDriverId());
        order.setVehicleId(request.getVehicleId());

        // Cambiar estado a ASSIGNMENT_PENDING para indicar que está esperando
        // validación
        order.setStatus(OrderStatus.ASSIGNMENT_PENDING);
        order.setSagaStep(SagaStep.WAITING_ASSIGNMENT_VERIFICATION);
        order.setAssignmentSagaStartedAt(LocalDateTime.now());
        Order savedOrder = orderRepository.save(order);

        log.info("Orden {} guardada con estado ASSIGNMENT_PENDING. Solicitando validación a FleetService", orderId);

        // Publicar evento de validación a FleetService
        // Si esto falla, la transacción se revierte y la orden vuelve a CREATED
        try {
            orderEventProducer.publishAssignValidationRequestEvent(savedOrder);
        } catch (Exception e) {
            log.error("Error publicando evento de validación para orderId={}", orderId, e);
            throw new RuntimeException("Error al solicitar validación de recursos", e);
        }

        return OrderMapper.entityToOrderResponse(savedOrder);
    }

    @Override
    @Transactional
    public void cancelOrderDueToVerificationFailure(UUID orderId, String reason) {
        log.warn("► [COMPENSATION] Cancelando orden por fallo de verificación de cliente: orderId={}, reason={}",
                orderId, reason);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado: " + orderId));

        if (!canCancelOrder(order.getStatus())) {
            log.warn("✗ No se puede cancelar orden en estado: {}", order.getStatus());
            throw new IllegalStateException(
                    "Solo se pueden cancelar órdenes en estado: PENDING, ASSIGNMENT_PENDING, ASSIGNED, PICKED_UP o IN_ROUTE. "
                            +
                            "Estado actual: " + order.getStatus());
        }

        log.info("✓ [COMPENSATION] Cancelando pedido: {}", orderId);
        order.setStatus(OrderStatus.CANCELLED);
        Order cancelledOrder = orderRepository.save(order);

        // Publicar evento de cancelación para que otros microservicios reaccionen
        try {
            orderEventProducer.publishOrderCancelledEvent(cancelledOrder);
            log.info("✓ [COMPENSATION] Evento de cancelación publicado para orderId={}", orderId);
        } catch (Exception e) {
            log.error("✗ Error publicando evento de cancelación para orderId={}", orderId, e);
            throw new RuntimeException("Error al publicar evento de cancelación", e);
        }
    }

    @Override
    @Transactional
    public void confirmOrderVerification(UUID orderId) {
        log.info("✓ [VERIFICATION] Confirmando verificación de customer para orden: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado: " + orderId));

        if (!OrderStatus.PENDING.equals(order.getStatus())) {
            log.warn("✗ Orden no está en estado PENDING para confirmar: estado actual={}", order.getStatus());
            throw new IllegalStateException(
                    "Solo se pueden confirmar órdenes en estado PENDING. " +
                            "Estado actual: " + order.getStatus());
        }

        log.info("✓ [VERIFICATION] Cambiando orden {} a CREATED (verificada)", orderId);
        order.setStatus(OrderStatus.CREATED);
        order.setSagaStep(SagaStep.VERIFIED);
        orderRepository.save(order);

        // Publicar notificación de orden creada exitosamente
        orderEventProducer.publishOrderConfirmedEvent(order);
    }

    /**
     * Recalcula los valores del pedido basándose en las direcciones actuales.
     * Valida que la distancia esté dentro de cobertura y actualiza:
     * - Distancia
     * - Trip Fee
     * - Service Fee
     * - Total Amount
     */
    private void calculateOrderValues(Order order, VehicleType vehicleType) {

        double distance = OrderUtils.calculateDistance(order.getPickupAddress(), order.getDeliveryAddress());
        BigDecimal distanceBd = BigDecimal.valueOf(distance).setScale(2, RoundingMode.HALF_UP);

        if (distance > OrderUtils.MAX_COVERAGE)
            throw new IllegalArgumentException("La distancia excede la cobertura operativa (Max 50km).");

        Tariff tariff = tariffRepository.findByVehicleType(vehicleType)
                .orElseThrow(() -> new IllegalArgumentException("No hay tarifa configurada para este vehículo"));

        BigDecimal tripFee = OrderUtils.calculateTripFee(tariff, distanceBd);
        BigDecimal serviceFee = tariff.getBaseCost();
        BigDecimal totalAmount = tripFee.add(serviceFee);

        order.setDistanceKm(distanceBd);
        order.setTripFee(tripFee);
        order.setServiceFee(serviceFee);
        order.setTotalAmount(totalAmount);
        order.setStatus(OrderStatus.PENDING); // Esperando verificación de customer
    }

    // Clase utilitaria interna
    private final class OrderUtils {

        private OrderUtils() {
            throw new UnsupportedOperationException("Clase utilitaria");
        }

        private static final int EARTH_RADIUS_KM = 6371;
        public static final int MAX_COVERAGE = 50;

        public static double calculateDistance(Address addr1, Address addr2) {

            if (addr1 == null || addr2 == null)
                throw new IllegalArgumentException("Las direcciones de origen y destino no pueden ser nulas.");

            if (addr1.getCoordinates() == null || addr2.getCoordinates() == null)
                throw new IllegalArgumentException("Las coordenadas de las direcciones no pueden ser nulas.");

            double lat1 = addr1.getCoordinates().getLatitude();
            double lon1 = addr1.getCoordinates().getLongitude();
            double lat2 = addr2.getCoordinates().getLatitude();
            double lon2 = addr2.getCoordinates().getLongitude();

            if (!isValidCoordinate(lat1, lon1) || !isValidCoordinate(lat2, lon2))
                throw new IllegalArgumentException("Las coordenadas proporcionadas son inválidas.");

            double dLat = Math.toRadians(lat2 - lat1);
            double dLon = Math.toRadians(lon2 - lon1);

            double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                    Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                            Math.sin(dLon / 2) * Math.sin(dLon / 2);

            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

            return EARTH_RADIUS_KM * c;

        }

        public static BigDecimal calculateTripFee(Tariff tariff, BigDecimal distance) {

            if (tariff == null)
                throw new IllegalArgumentException("La tarifa no puede ser nula.");

            if (distance == null)
                throw new IllegalArgumentException("La distancia no puede ser nula.");

            if (distance.compareTo(BigDecimal.ZERO) < 0)
                throw new IllegalArgumentException("La distancia no puede ser negativa.");

            if (distance.compareTo(BigDecimal.valueOf(tariff.getMinDistanceKm())) < 0) {
                return tariff.getBaseCost();
            }
            return distance.multiply(tariff.getCostPerKm());

        }

        private static boolean isValidCoordinate(double latitude, double longitude) {
            return latitude >= -90 && latitude <= 90 && longitude >= -180 && longitude <= 180;
        }
    }

    @Override
    @Transactional
    public void dropOrderDueToRejection(UUID orderId, String reason) {
        log.info("✗ [REJECTION] Descartando orden por rechazo de verificación: orderId={}, reason={}", orderId, reason);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado: " + orderId));

        order.setSagaStep(SagaStep.REJECTED);
        order.setSagaReason(reason);
        orderRepository.save(order);

        // Publicar notificación de rechazo
        orderEventProducer.publishOrderRejectedEvent(orderId, order.getCustomerId(), reason);

        // Eliminar la orden
        orderRepository.deleteById(orderId);
        log.info("✓ Orden eliminada por rechazo: orderId={}", orderId);
    }

    @Override
    @Transactional
    public void dropOrderDueToTimeout(UUID orderId) {
        log.warn("✗ [TIMEOUT] Cancelando orden por timeout en verificación: orderId={}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado: " + orderId));

        order.setSagaStep(SagaStep.TIMEOUT);
        order.setSagaReason("Timeout en verificación de cliente");
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        // Publicar notificación de timeout (como cancelled)
        orderEventProducer.publishOrderTimeoutEvent(orderId, order.getCustomerId());

        log.info("✓ Orden cancelada por timeout: orderId={}", orderId);
    }

    @Override
    @Transactional
    public void confirmAssignmentVerification(UUID orderId) {
        log.info("✓ [FLEET-VERIFICATION] Confirmando validación de asignación para orden: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado: " + orderId));

        if (!OrderStatus.ASSIGNMENT_PENDING.equals(order.getStatus())) {
            log.warn("✗ Orden no está en estado ASSIGNMENT_PENDING: estado actual={}", order.getStatus());
            throw new IllegalStateException(
                    "Solo se pueden confirmar órdenes en estado ASSIGNMENT_PENDING. " +
                            "Estado actual: " + order.getStatus());
        }

        log.info("✓ [FLEET-VERIFICATION] Cambiando orden {} a ASSIGNED (asignación confirmada)", orderId);
        order.setStatus(OrderStatus.ASSIGNED);
        order.setSagaStep(SagaStep.ASSIGNMENT_VERIFIED);
        orderRepository.save(order);

        // Publicar notificación de asignación exitosa
        orderEventProducer.publishOrderAssignedEvent(order);
    }

    @Override
    @Transactional
    public void rejectAssignmentAndReturnToCreated(UUID orderId, String reason) {
        log.warn("✗ [FLEET-VERIFICATION] Retornando orden a CREATED por fallo de validación: orderId={}, reason={}",
                orderId, reason);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado: " + orderId));

        if (!OrderStatus.ASSIGNMENT_PENDING.equals(order.getStatus())) {
            log.warn("✗ Orden no está en estado ASSIGNMENT_PENDING para revertir: estado actual={}", order.getStatus());
            throw new IllegalStateException(
                    "Solo se pueden revertir órdenes en estado ASSIGNMENT_PENDING. " +
                            "Estado actual: " + order.getStatus());
        }

        log.info("✓ [FLEET-VERIFICATION] Revirtiendo asignación - retornando a CREATED: {}", orderId);

        // Limpiar los datos de asignación temporal
        order.setDriverId(null);
        order.setVehicleId(null);

        // Retornar a estado CREATED
        order.setStatus(OrderStatus.CREATED);
        order.setSagaReason(reason);
        orderRepository.save(order);

        // Publicar notificación de fallo de asignación
        orderEventProducer.publishAssignmentFailedEvent(orderId, order.getCustomerId(), reason);

        log.info("✓ Orden revertida a CREATED: orderId={}", orderId);
    }

    // ==================== SAGA DE CANCELACIÓN ====================

    @Override
    @Transactional
    public void confirmCancellation(UUID orderId) {
        log.info("✓ [CANCELLATION-SAGA] FleetService confirmó liberación de recursos para orden: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado: " + orderId));

        if (!OrderStatus.CANCELLATION_PENDING.equals(order.getStatus())) {
            log.warn("✗ Orden no está en estado CANCELLATION_PENDING: estado actual={}", order.getStatus());
            throw new IllegalStateException(
                    "Solo se pueden confirmar cancelaciones de órdenes en estado CANCELLATION_PENDING. " +
                            "Estado actual: " + order.getStatus());
        }

        log.info("✓ [CANCELLATION-SAGA] Cancelación confirmada. Orden {} → CANCELLED", orderId);
        order.setStatus(OrderStatus.CANCELLED);
        order.setSagaStep(SagaStep.CANCELLATION_VERIFIED);
        order.setDriverId(null);
        order.setVehicleId(null);
        order.setPreviousStatus(null);
        orderRepository.save(order);

        // Publicar notificación de cancelación exitosa
        orderEventProducer.publishOrderCancelledEvent(order);
    }

    @Override
    @Transactional
    public void rejectCancellationAndRestore(UUID orderId, String reason) {
        log.warn("✗ [CANCELLATION-SAGA] FleetService rechazó la cancelación: orderId={}, reason={}", orderId, reason);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado: " + orderId));

        if (!OrderStatus.CANCELLATION_PENDING.equals(order.getStatus())) {
            log.warn("✗ Orden no está en estado CANCELLATION_PENDING para revertir: estado actual={}",
                    order.getStatus());
            throw new IllegalStateException(
                    "Solo se pueden revertir cancelaciones de órdenes en estado CANCELLATION_PENDING. " +
                            "Estado actual: " + order.getStatus());
        }

        // Restaurar el estado previo
        OrderStatus previousStatus = order.getPreviousStatus() != null
                ? order.getPreviousStatus()
                : OrderStatus.ASSIGNED;

        log.info("✓ [CANCELLATION-SAGA] Restaurando orden {} al estado previo: {}", orderId, previousStatus);
        order.setStatus(previousStatus);
        order.setPreviousStatus(null);
        order.setSagaReason("Cancelación rechazada: " + reason);
        order.setCancellationSagaStartedAt(null);
        orderRepository.save(order);

        log.info("✓ Orden restaurada a {}: orderId={}", previousStatus, orderId);
    }

    @Override
    @Transactional
    public void cancelOrderDueToCancellationTimeout(UUID orderId) {
        log.warn("✗ [CANCELLATION-TIMEOUT] Timeout en saga de cancelación: orderId={}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado: " + orderId));

        if (!OrderStatus.CANCELLATION_PENDING.equals(order.getStatus())) {
            log.warn("✗ Orden no está en CANCELLATION_PENDING para timeout: estado={}", order.getStatus());
            return;
        }

        // En timeout, forzamos cancelación para proteger al usuario
        log.info("✓ [CANCELLATION-TIMEOUT] Forzando cancelación por timeout: orderId={}", orderId);
        order.setStatus(OrderStatus.CANCELLED);
        order.setSagaStep(SagaStep.CANCELLATION_TIMEOUT);
        order.setSagaReason("Timeout en validación de cancelación con FleetService");
        order.setDriverId(null);
        order.setVehicleId(null);
        order.setPreviousStatus(null);
        orderRepository.save(order);

        orderEventProducer.publishOrderCancelledEvent(order);
    }

    // ==================== SAGA DE PICKUP ====================

    @Override
    @Transactional
    public void confirmPickup(UUID orderId) {
        log.info("✓ [PICKUP-SAGA] FleetService confirmó validación de pickup para orden: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado: " + orderId));

        if (!OrderStatus.PICKUP_PENDING.equals(order.getStatus())) {
            log.warn("✗ Orden no está en estado PICKUP_PENDING: estado actual={}", order.getStatus());
            throw new IllegalStateException(
                    "Solo se pueden confirmar pickups de órdenes en estado PICKUP_PENDING. " +
                            "Estado actual: " + order.getStatus());
        }

        log.info("✓ [PICKUP-SAGA] Pickup confirmado. Orden {} → PICKED_UP", orderId);
        order.setStatus(OrderStatus.PICKED_UP);
        order.setSagaStep(SagaStep.PICKUP_VERIFIED);
        orderRepository.save(order);

        orderEventProducer.publishOrderPickedUpEvent(order);
    }

    @Override
    @Transactional
    public void rejectPickupAndReturnToAssigned(UUID orderId, String reason) {
        log.warn("✗ [PICKUP-SAGA] FleetService rechazó el pickup: orderId={}, reason={}", orderId, reason);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado: " + orderId));

        if (!OrderStatus.PICKUP_PENDING.equals(order.getStatus())) {
            log.warn("✗ Orden no está en estado PICKUP_PENDING para revertir: estado actual={}", order.getStatus());
            throw new IllegalStateException(
                    "Solo se pueden revertir pickups de órdenes en estado PICKUP_PENDING. " +
                            "Estado actual: " + order.getStatus());
        }

        log.info("✓ [PICKUP-SAGA] Retornando orden {} a ASSIGNED", orderId);
        order.setStatus(OrderStatus.ASSIGNED);
        order.setSagaStep(SagaStep.PICKUP_REJECTED);
        order.setSagaReason("Pickup rechazado: " + reason);
        order.setPickupSagaStartedAt(null);
        orderRepository.save(order);

        log.info("✓ Orden revertida a ASSIGNED: orderId={}", orderId);
    }

    @Override
    @Transactional
    public void rejectPickupDueToTimeout(UUID orderId) {
        log.warn("✗ [PICKUP-TIMEOUT] Timeout en saga de pickup: orderId={}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado: " + orderId));

        if (!OrderStatus.PICKUP_PENDING.equals(order.getStatus())) {
            log.warn("✗ Orden no está en PICKUP_PENDING para timeout: estado={}", order.getStatus());
            return;
        }

        // En timeout de pickup, retornamos a ASSIGNED (estado seguro)
        log.info("✓ [PICKUP-TIMEOUT] Retornando a ASSIGNED por timeout: orderId={}", orderId);
        order.setStatus(OrderStatus.ASSIGNED);
        order.setSagaStep(SagaStep.PICKUP_TIMEOUT);
        order.setSagaReason("Timeout en validación de pickup con FleetService");
        order.setPickupSagaStartedAt(null);
        orderRepository.save(order);
    }
}