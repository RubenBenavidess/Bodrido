package com.espe.edu.ec.order_ms.event_listeners;

import java.util.UUID;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import com.espe.edu.ec.order_ms.config.RabbitMQConfig;
import com.espe.edu.ec.order_ms.model_enums.OrderStatus;
import com.espe.edu.ec.order_ms.models.Order;
import com.espe.edu.ec.order_ms.repositories.OrderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {

    private final OrderRepository orderRepository;

    /**
     * Escucha eventos de validación desde otros microservicios (FleetService, etc)
     */
    @RabbitListener(queues = RabbitMQConfig.VALIDATION_QUEUE_NAME)
    public void handleValidationEvent(OrderValidationEvent event) {
        log.info("Evento de validación recibido: orderId={}, validationType={}, success={}", 
            event.getOrderId(), event.getValidationType(), event.isSuccess());

        if (event.getOrderId() == null) {
            log.warn("Evento de validación sin orderId, ignorando");
            return;
        }

        try {
            if (event.isSuccess()) {
                switch (event.getValidationType()) {
                    case "resources_assigned":
                        handleResourcesValidated(event.getOrderId());
                        break;
                    default:
                        log.debug("Tipo de validación no manejado: {}", event.getValidationType());
                }
            } else {
                log.warn("Validación fallida para orden {}: {}", event.getOrderId(), event.getErrorMessage());
                handleValidationFailed(event.getOrderId(), event.getErrorMessage());
            }
        } catch (Exception e) {
            log.error("Error procesando evento de validación: orderId={}", event.getOrderId(), e);
        }
    }

    /**
     * Cambia el estado de la orden a ASSIGNED cuando FleetService valida los recursos
     */
    private void handleResourcesValidated(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado: " + orderId));

        if (!OrderStatus.CREATED.equals(order.getStatus())) {
            log.warn("Orden {} no está en estado CREATED, estado actual: {}", orderId, order.getStatus());
            return;
        }

        order.setStatus(OrderStatus.ASSIGNED);
        orderRepository.save(order);
        log.info("Orden {} actualizada a estado ASSIGNED por validación de FleetService", orderId);
    }

    /**
     * Maneja fallos en las validaciones
     */
    private void handleValidationFailed(UUID orderId, String errorMessage) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado: " + orderId));

        log.warn("Validación fallida para orden {}: {}", orderId, errorMessage);
        // Aquí puedes implementar lógica para revertir estados o notificar al cliente
    }
}
