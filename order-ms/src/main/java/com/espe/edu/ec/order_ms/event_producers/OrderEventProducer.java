package com.espe.edu.ec.order_ms.event_producers;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.espe.edu.ec.order_ms.config.RabbitMQConfig;
import com.espe.edu.ec.order_ms.dtos.events.OrderCreatedEvent;
import com.espe.edu.ec.order_ms.dtos.events.OrderNotificationEvent;
import com.espe.edu.ec.order_ms.dtos.events.OrderValidationEvent;
import com.espe.edu.ec.order_ms.models.Order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventProducer {

    private final RabbitTemplate rabbitTemplate;

    private static final String ACTION_CREATE = "create";
    private static final String ACTION_REJECTED = "rejected";
    private static final String ACTION_ASSIGNED = "assigned";
    private static final String ACTION_CANCELLED = "cancelled";
    private static final String ACTION_PATCH = "patched";
    private static final String ACTION_PICKED_UP = "picked_up";

    private static final String ACTION_VALIDATE_RESOURCES = "validate_resources";

    private static final String SEVERITY_INFO = "INFO";

    // ----------------------------------------
    // PUBLISH NOTIFICATIONS
    // ----------------------------------------

    /**
     * Publica notificación de orden creada
     */
    public void publishOrderCreatedEvent(Order order) {
        Map<String, Object> data = new HashMap<>();
        data.put("customerId", order.getCustomerId());
        data.put("customerTaxId", order.getCustomerTaxId());
        data.put("distanceKm", order.getDistanceKm());
        data.put("tripFee", order.getTripFee());
        data.put("serviceFee", order.getServiceFee());
        data.put("totalAmount", order.getTotalAmount());
        data.put("status", order.getStatus().toString());

        sendNotification(order.getId(), "order_pending", "Orden creada - Esperando verificación de cliente", data);

        publishCustomerVerificationEvent(order);
    }

    /**
     * Cambia los datos de una orden creada
     */
    public void publishOrderPatchedEvent(Order order) {
        Map<String, Object> data = new HashMap<>();
        data.put("customerId", order.getCustomerId());
        data.put("distanceKm", order.getDistanceKm());
        data.put("tripFee", order.getTripFee());
        data.put("serviceFee", order.getServiceFee());
        data.put("totalAmount", order.getTotalAmount());
        data.put("status", order.getStatus().toString());
        data.put("deliveryAddress", order.getDeliveryAddress());

        sendNotification(order.getId(), ACTION_PATCH, "Orden actualizada exitosamente", data);
    }

    /**
     * Publica notificación de orden recogida
     */
    public void publishOrderPickedUpEvent(Order order) {
        Map<String, Object> data = new HashMap<>();

        data.put("driverId", order.getDriverId());
        data.put("vehicleId", order.getVehicleId());
        data.put("customerId", order.getCustomerId());
        data.put("status", order.getStatus().toString());

        sendNotification(order.getId(), ACTION_PICKED_UP, "Orden recogida exitosamente", data);
    }

    /**
     * Publica notificación de asignación de conductor y vehículo
     */
    public void publishOrderAssignedEvent(Order order) {
        Map<String, Object> data = new HashMap<>();
        data.put("driverId", order.getDriverId());
        data.put("vehicleId", order.getVehicleId());
        data.put("status", order.getStatus().toString());

        sendNotification(order.getId(), ACTION_ASSIGNED, "Conductor y vehículo asignados", data);
    }

    /**
     * Publica notificación de fallo en validación de asignación de conductor y
     * vehículo
     */
    public void publishAssignmentFailedEvent(UUID orderId, UUID customerId, String reason) {
        Map<String, Object> data = new HashMap<>();
        data.put("customerId", customerId);
        data.put("reason", reason);
        data.put("failedAt", LocalDateTime.now());

        sendNotification(orderId, "assignment_failed", "Asignación de recursos rechazada - " + reason, data);
    }

    /**
     * Publica notificación de cancelación de orden
     */
    public void publishOrderCancelledEvent(Order order) {
        Map<String, Object> data = new HashMap<>();
        data.put("customerId", order.getCustomerId());
        data.put("status", order.getStatus().toString());
        data.put("cancelledAt", LocalDateTime.now());

        sendNotification(order.getId(), ACTION_CANCELLED, "Orden cancelada - Verificación de cliente fallida", data);
    }

    /**
     * Publica notificación de orden rechazada (verificación fallida)
     */
    public void publishOrderRejectedEvent(UUID orderId, UUID customerId, String reason) {
        Map<String, Object> data = new HashMap<>();
        data.put("customerId", customerId);
        data.put("reason", reason);
        data.put("rejectedAt", LocalDateTime.now());

        sendNotification(orderId, ACTION_REJECTED, "Orden rechazada - " + reason, data);
    }

    /**
     * Publica notificación de orden descartada por timeout (como cancelada)
     */
    public void publishOrderTimeoutEvent(UUID orderId, UUID customerId) {
        Map<String, Object> data = new HashMap<>();
        data.put("customerId", customerId);
        data.put("timeoutAt", LocalDateTime.now());
        data.put("reason", "Timeout en verificación de cliente");

        sendNotification(orderId, ACTION_CANCELLED, "Orden cancelada - Timeout en verificación de cliente", data);
    }

    /**
     * Publica notificación de orden confirmada (verificación exitosa)
     */
    public void publishOrderConfirmedEvent(Order order) {
        Map<String, Object> data = new HashMap<>();
        data.put("customerId", order.getCustomerId());
        data.put("customerTaxId", order.getCustomerTaxId());
        data.put("distanceKm", order.getDistanceKm());
        data.put("tripFee", order.getTripFee());
        data.put("serviceFee", order.getServiceFee());
        data.put("totalAmount", order.getTotalAmount());
        data.put("status", order.getStatus().toString());

        sendNotification(order.getId(), ACTION_CREATE, "Orden creada - Verificación de cliente exitosa", data);
    }

    // ----------------------------------------
    // PUBLISH FLEET VALIDATIONS
    // ----------------------------------------

    /**
     * Publica validación de asignación de conductor y vehículo a fleet-ms
     */
    public void publishAssignValidationRequestEvent(Order order) {
        Map<String, Object> data = new HashMap<>();
        data.put("orderId", order.getId());
        data.put("driverId", order.getDriverId());
        data.put("vehicleId", order.getVehicleId());
        data.put("status", order.getStatus().toString());

        OrderValidationEvent orderValidationEvent = OrderValidationEvent.builder()
                .orderId(order.getId())
                .driverId(order.getDriverId())
                .vehicleId(order.getVehicleId())
                .validationType("ASSIGNMENT_VALIDATION")
                .sourceService("order-ms")
                .timestamp(LocalDateTime.now())
                .build();

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.ORDERS_VALIDATIONS_FLEET_EXCHANGE,
                    RabbitMQConfig.ORDERS_VALIDATIONS_FLEET_ROUTING_KEY,
                    orderValidationEvent);
            log.info("✓ Evento de validación de asignación publicado para fleet-ms: orderId={}", order.getId());
        } catch (Exception e) {
            log.error("✗ Error publicando evento de validación para fleet-ms: orderId={}", order.getId(), e);
            throw new RuntimeException("Error al solicitar validación de recursos", e);
        }

        // También publica notificación local
        sendNotification(
                order.getId(),
                ACTION_VALIDATE_RESOURCES,
                "Solicitando validación de disponibilidad de conductor y vehículo",
                data);
    }

    /**
     * Publica solicitud de cancelación a fleet-ms para liberar conductor y
     * vehículo.
     * FleetService debe verificar y liberar los recursos, luego responder con
     * éxito/fallo.
     */
    public void publishCancellationValidationEvent(Order order) {
        OrderValidationEvent event = OrderValidationEvent.builder()
                .orderId(order.getId())
                .driverId(order.getDriverId())
                .vehicleId(order.getVehicleId())
                .validationType("CANCELLATION_VALIDATION")
                .sourceService("order-ms")
                .timestamp(LocalDateTime.now())
                .build();

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.ORDERS_VALIDATIONS_FLEET_EXCHANGE,
                    RabbitMQConfig.ORDERS_VALIDATIONS_FLEET_ROUTING_KEY,
                    event);
            log.info("✓ Evento de cancelación publicado para fleet-ms: orderId={}, driverId={}, vehicleId={}",
                    order.getId(), order.getDriverId(), order.getVehicleId());
        } catch (Exception e) {
            log.error("✗ Error publicando evento de cancelación para fleet-ms: orderId={}", order.getId(), e);
            throw new RuntimeException("Error al solicitar liberación de recursos para cancelación", e);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("orderId", order.getId());
        data.put("driverId", order.getDriverId());
        data.put("vehicleId", order.getVehicleId());
        data.put("status", order.getStatus().toString());
        sendNotification(order.getId(), "cancellation_requested",
                "Solicitando liberación de conductor y vehículo para cancelación", data);
    }

    /**
     * Publica solicitud de validación de pickup a fleet-ms.
     * FleetService debe verificar que el conductor y vehículo siguen
     * BUSY/asignados.
     */
    public void publishPickupValidationEvent(Order order) {
        OrderValidationEvent event = OrderValidationEvent.builder()
                .orderId(order.getId())
                .driverId(order.getDriverId())
                .vehicleId(order.getVehicleId())
                .validationType("PICKUP_VALIDATION")
                .sourceService("order-ms")
                .timestamp(LocalDateTime.now())
                .build();

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.ORDERS_VALIDATIONS_FLEET_EXCHANGE,
                    RabbitMQConfig.ORDERS_VALIDATIONS_FLEET_ROUTING_KEY,
                    event);
            log.info("✓ Evento de validación de pickup publicado para fleet-ms: orderId={}, driverId={}, vehicleId={}",
                    order.getId(), order.getDriverId(), order.getVehicleId());
        } catch (Exception e) {
            log.error("✗ Error publicando evento de validación de pickup para fleet-ms: orderId={}", order.getId(), e);
            throw new RuntimeException("Error al solicitar validación de pickup", e);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("orderId", order.getId());
        data.put("driverId", order.getDriverId());
        data.put("vehicleId", order.getVehicleId());
        data.put("status", order.getStatus().toString());
        sendNotification(order.getId(), "pickup_validation_requested",
                "Solicitando validación de pickup con fleet-ms", data);
    }

    // ----------------------------------------
    // PUBLISH CUSTOMER VALIDATIONS
    // ----------------------------------------

    /**
     * Publica evento de verificación de cliente a CUSTOMER-MS
     */
    private void publishCustomerVerificationEvent(Order order) {
        try {
            OrderCreatedEvent event = OrderCreatedEvent.builder()
                    .orderId(order.getId())
                    .customerId(order.getCustomerId())
                    .action("order_created")
                    .message("New order created for customer verification")
                    .timestamp(System.currentTimeMillis())
                    .build();

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.ORDERS_VALIDATIONS_CUSTOMER_EXCHANGE,
                    RabbitMQConfig.ORDERS_VALIDATIONS_CUSTOMER_ROUTING_KEY,
                    event);

            log.info("✓ Evento de verificación publicado para customer-ms: orderId={}, customerId={}",
                    order.getId(), order.getCustomerId());
        } catch (Exception e) {
            log.error("✗ Error publicando evento de verificación para customer-ms: orderId={}",
                    order.getId(), e);
            // No lanzar excepción - la orden ya fue creada, solo falló el evento
        }
    }

    /**
     * Envía la notificación de orden
     */
    private void sendNotification(UUID orderId, String action, String message, Map<String, Object> data) {
        OrderNotificationEvent notification = OrderNotificationEvent.builder()
                .orderId(orderId)
                .action(action)
                .message(message)
                .timestamp(LocalDateTime.now())
                .data(data)
                .severity(SEVERITY_INFO)
                .build();

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    RabbitMQConfig.ROUTING_KEY,
                    notification);
            log.info("Notificación publicada: orderId={}, action={}", orderId, action);
        } catch (Exception e) {
            log.error("Error al publicar notificación: orderId={}, action={}", orderId, action, e);
            throw new RuntimeException("Error al publicar notificación", e);
        }
    }
}
