package com.espe.edu.ec.order_ms.event_listeners;

import com.espe.edu.ec.order_ms.config.RabbitMQConfig;
import com.espe.edu.ec.order_ms.dtos.events.OrderValidationEvent;
import com.espe.edu.ec.order_ms.services.OrderService;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Listener que procesa las respuestas de FleetService para las sagas
 * de cancelación y pickup. Comparte la misma cola que
 * AssignmentVerificationResultListener
 * (order-fleet.verification.result), diferenciándose por validationType.
 *
 * NOTA: Este listener reemplaza la lógica del
 * AssignmentVerificationResultListener,
 * unificando todo el manejo de resultados de fleet en un solo lugar.
 */
@Slf4j
@Component
public class FleetVerificationResultListener {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    public FleetVerificationResultListener(OrderService orderService) {
        this.orderService = orderService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
    }

    @RabbitListener(queues = RabbitMQConfig.ORDER_FLEET_VERIFICATION_RESULT_QUEUE)
    public void handleFleetVerificationResult(Message message) {
        try {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            log.info("► [FLEET-RESULT] Mensaje raw recibido: {}", body);

            OrderValidationEvent event = objectMapper.readValue(body, OrderValidationEvent.class);

            if (event.getOrderId() == null) {
                log.error("✗ [FLEET-RESULT] orderId es null después de deserializar. Body: {}", body);
                return;
            }

            String validationType = event.getValidationType() != null
                    ? event.getValidationType()
                    : "resources_assigned"; // default para compatibilidad con el flujo existente

            log.info("► [FLEET-RESULT] Resultado recibido: OrderId={}, ValidationType={}, Success={}",
                    event.getOrderId(), validationType, event.isSuccess());

            switch (validationType) {
                case "resources_assigned" -> handleAssignmentResult(event);
                case "cancellation_result" -> handleCancellationResult(event);
                case "pickup_result" -> handlePickupResult(event);
                default -> log.warn("✗ [FLEET-RESULT] ValidationType desconocido: {}", validationType);
            }

        } catch (Exception e) {
            log.error("✗ Error procesando resultado de verificación de fleet: {}", e.getMessage(), e);
        }
    }

    private void handleAssignmentResult(OrderValidationEvent event) {
        if (event.isSuccess()) {
            log.info("✓ [ASSIGNMENT] Validación EXITOSA - Confirmando asignación: {}", event.getOrderId());
            orderService.confirmAssignmentVerification(event.getOrderId());
        } else {
            log.warn("✗ [ASSIGNMENT] Validación FALLIDA - Retornando a CREATED: {}", event.getOrderId());
            String reason = event.getErrorMessage() != null
                    ? event.getErrorMessage()
                    : "Assignment validation failed";
            orderService.rejectAssignmentAndReturnToCreated(event.getOrderId(), reason);
        }
    }

    private void handleCancellationResult(OrderValidationEvent event) {
        if (event.isSuccess()) {
            log.info("✓ [CANCELLATION] Recursos liberados - Confirmando cancelación: {}", event.getOrderId());
            orderService.confirmCancellation(event.getOrderId());
        } else {
            log.warn("✗ [CANCELLATION] No se pudieron liberar recursos - Restaurando orden: {}", event.getOrderId());
            String reason = event.getErrorMessage() != null
                    ? event.getErrorMessage()
                    : "Fleet could not release resources";
            orderService.rejectCancellationAndRestore(event.getOrderId(), reason);
        }
    }

    private void handlePickupResult(OrderValidationEvent event) {
        if (event.isSuccess()) {
            log.info("✓ [PICKUP] Validación EXITOSA - Confirmando pickup: {}", event.getOrderId());
            orderService.confirmPickup(event.getOrderId());
        } else {
            log.warn("✗ [PICKUP] Validación FALLIDA - Retornando a ASSIGNED: {}", event.getOrderId());
            String reason = event.getErrorMessage() != null
                    ? event.getErrorMessage()
                    : "Pickup validation failed";
            orderService.rejectPickupAndReturnToAssigned(event.getOrderId(), reason);
        }
    }
}
