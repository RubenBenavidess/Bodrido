package com.espe.edu.ec.order_ms.event_producers;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.espe.edu.ec.order_ms.config.RabbitMQConfig;
import com.espe.edu.ec.order_ms.models.Order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventProducer {

    private final RabbitTemplate rabbitTemplate;

    private static final String ACTION_CREATE = "create";
    private static final String ACTION_ASSIGNED = "assigned";
    private static final String ACTION_CANCELLED = "cancelled";
    private static final String ACTION_PATCH = "patched";
    private static final String ACTION_PICKED_UP = "picked_up";
    
    private static final String ACTION_VALIDATE_RESOURCES = "validate_resources";

    private static final String SEVERITY_INFO = "INFO";

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
        
        sendNotification(order.getId(), ACTION_CREATE, "Orden creada exitosamente", data);
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
     * Publica notificación de orden asignada en progreso
     */
    public void publishValidationRequestEvent(Order order) {
        Map<String, Object> data = new HashMap<>();
        // Datos cruciales para que el otro MS pueda trabajar
        data.put("driverId", order.getDriverId());
        data.put("vehicleId", order.getVehicleId());
        data.put("status", order.getStatus().toString());

        // Usamos el método sendNotification que ya construiste
        sendNotification(
            order.getId(), 
            ACTION_VALIDATE_RESOURCES, 
            "Solicitando validación de disponibilidad de conductor y vehículo", 
            data
    );
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
     * Publica notificación de cancelación de orden
     */
    public void publishOrderCancelledEvent(Order order) {
        Map<String, Object> data = new HashMap<>();
        data.put("customerId", order.getCustomerId());
        data.put("status", order.getStatus().toString());
        
        sendNotification(order.getId(), ACTION_CANCELLED, "Orden cancelada", data);
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
                notification
            );
            log.info("Notificación publicada: orderId={}, action={}", orderId, action);
        } catch (Exception e) {
            log.error("Error al publicar notificación: orderId={}, action={}", orderId, action, e);
            throw new RuntimeException("Error al publicar notificación", e);
        }
    }
}
