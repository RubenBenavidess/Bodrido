package com.espe.edu.ec.order_ms.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    public boolean orderExists(UUID id){
        return getOrder(id) == null ? false : true;
    }

    @Override
    @Transactional
    public OrderResponse patchOrder(UUID id, OrderPatchRequest orderPatchRequest) {
        
        Order foundOrder = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado: " + id));

        if (!OrderStatus.CREATED.equals(foundOrder.getStatus()))
            throw new IllegalStateException("Solo se pueden editar pedidos en estado CREATED");
        
        if(orderPatchRequest instanceof DeliveryOrderPatchRequest deliveryOrderPatchRequest){

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
                "CREATED, ASSIGNMENT_PENDING, ASSIGNED, PICKED_UP o IN_ROUTE. " +
                "Estado actual: " + foundOrder.getStatus());
        }

        log.info("Cancelando pedido: {}", id);
        foundOrder.setStatus(OrderStatus.CANCELLED);
        Order cancelledOrder = orderRepository.save(foundOrder);

        // Publicar evento de cancelación para que FleetService revierte los cambios
        try {
            orderEventProducer.publishOrderCancelledEvent(cancelledOrder);
        } catch (Exception e) {
            log.error("Error publicando evento de cancelación para orderId={}", id, e);
            throw new RuntimeException("Error al publicar evento de cancelación", e);
        }
    }

    /**
     * Valida si una orden puede ser cancelada según su estado actual
     */
    private boolean canCancelOrder(OrderStatus status) {
        return OrderStatus.CREATED.equals(status) ||
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

        if (!OrderStatus.CREATED.equals(foundOrder.getStatus()))
            throw new IllegalStateException("La orden debe estar en estado CREATED para ser recogida.");

        foundOrder.setStatus(OrderStatus.PICKED_UP);
        Order updatedOrder = orderRepository.save(foundOrder);

        orderEventProducer.publishOrderPickedUpEvent(updatedOrder);
        return OrderMapper.entityToOrderResponse(updatedOrder);
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

        // Validar que la orden está en estado CREATED (solo en este estado se puede asignar)
        if (!OrderStatus.CREATED.equals(order.getStatus())) {
            throw new IllegalStateException(
                "No se puede asignar recursos. La orden debe estar en estado CREATED, " +
                "estado actual: " + order.getStatus());
        }

        // Guardar temporalmente los IDs que queremos validar
        order.setDriverId(request.getDriverId());
        order.setVehicleId(request.getVehicleId());
        
        // Cambiar estado a ASSIGNMENT_PENDING para indicar que está esperando validación
        order.setStatus(OrderStatus.ASSIGNMENT_PENDING);
        Order savedOrder = orderRepository.save(order);

        log.info("Orden {} guardada con estado ASSIGNMENT_PENDING. Solicitando validación a FleetService", orderId);

        // Publicar evento de validación a FleetService
        // Si esto falla, la transacción se revierte y la orden vuelve a CREATED
        try {
            orderEventProducer.publishValidationRequestEvent(savedOrder);
        } catch (Exception e) {
            log.error("Error publicando evento de validación para orderId={}", orderId, e);
            throw new RuntimeException("Error al solicitar validación de recursos", e);
        }

        return OrderMapper.entityToOrderResponse(savedOrder);
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
        order.setStatus(OrderStatus.CREATED);
    }

    // Clase utilitaria interna
    private final class OrderUtils {

        private OrderUtils(){
            throw new UnsupportedOperationException("Clase utilitaria");
        }

        private static final int EARTH_RADIUS_KM = 6371;
        public static final int MAX_COVERAGE = 50;

        public static double calculateDistance(Address addr1, Address addr2){

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
}