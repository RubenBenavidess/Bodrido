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

@Slf4j
@Component
public class AssignmentVerificationResultListener {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    public AssignmentVerificationResultListener(OrderService orderService) {
        this.orderService = orderService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
    }

    @RabbitListener(queues = RabbitMQConfig.ORDER_FLEET_VERIFICATION_RESULT_QUEUE)
    public void handleAssignmentVerificationResult(Message message) {
        try {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            log.info("► [ASSIGNMENT-VERIFICATION] Mensaje raw recibido: {}", body);

            OrderValidationEvent event = objectMapper.readValue(body, OrderValidationEvent.class);

            log.info("► [ASSIGNMENT-VERIFICATION] Resultado deserializado: Order ID = {}, Success = {}",
                    event.getOrderId(), event.isSuccess());

            if (event.getOrderId() == null) {
                log.error("✗ [ASSIGNMENT-VERIFICATION] orderId es null después de deserializar. Body: {}", body);
                return;
            }

            if (event.isSuccess()) {
                log.info("✓ [ASSIGNMENT-VERIFICATION] Validación EXITOSA - Confirmando asignación: {}",
                        event.getOrderId());
                orderService.confirmAssignmentVerification(event.getOrderId());

            } else {
                log.warn("✗ [ASSIGNMENT-VERIFICATION] Validación FALLIDA - Retornando a CREATED: {}",
                        event.getOrderId());

                String reason = event.getErrorMessage() != null ? event.getErrorMessage()
                        : "Assignment validation failed";
                orderService.rejectAssignmentAndReturnToCreated(event.getOrderId(), reason);
            }

        } catch (Exception e) {
            log.error("✗ Error procesando resultado de verificación de asignación: {}", e.getMessage(), e);
        }
    }
}
