package com.espe.edu.ec.notification_ms.event_listeners;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.espe.edu.ec.notification_ms.config.RabbitMQConfig;
import com.espe.edu.ec.notification_ms.models.Notification;
import com.espe.edu.ec.notification_ms.repositories.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;

    /**
     * Escucha eventos de billing (invoice_created, invoice_issued)
     */
    @RabbitListener(queues = RabbitMQConfig.BILLING_NOTIFICATIONS_QUEUE)
    @Transactional
    public void handleBillingEvent(BillingNotificationEvent event) {
        log.info("Evento de billing recibido: invoiceId={}, action={}", event.getInvoiceId(), event.getAction());

        try {
            if (event.getInvoiceId() == null) {
                log.warn("Evento de billing sin invoiceId, ignorando");
                return;
            }

            String dataJson = objectMapper.writeValueAsString(event.getData());

            Notification notification = Notification.builder()
                    .microservice("billing-ms")
                    .action(event.getAction())
                    .entityType("INVOICE")
                    .entityId(event.getInvoiceId())
                    .message(event.getMessage())
                    .data(dataJson)
                    .severity(event.getSeverity() != null ? event.getSeverity() : "INFO")
                    .build();

            notificationRepository.save(notification);
            log.info("Notificación de billing guardada: invoiceId={}, action={}", event.getInvoiceId(), event.getAction());
        } catch (Exception e) {
            log.error("Error procesando evento de billing: invoiceId={}", event.getInvoiceId(), e);
        }
    }

    /**
     * Escucha eventos de validación de órdenes (FleetService)
     */
    @RabbitListener(queues = RabbitMQConfig.VALIDATIONS_NOTIFICATIONS_QUEUE)
    @Transactional
    public void handleValidationEvent(OrderValidationEvent event) {
        log.info("Evento de validación recibido: orderId={}, success={}", event.getOrderId(), event.isSuccess());

        try {
            if (event.getOrderId() == null) {
                log.warn("Evento de validación sin orderId, ignorando");
                return;
            }

            String action = event.isSuccess() ? "validation_success" : "validation_failed";
            String message = event.isSuccess() ? "Recursos validados correctamente" : event.getErrorMessage();
            String severity = event.isSuccess() ? "INFO" : "WARNING";

            Notification notification = Notification.builder()
                    .microservice("fleet-service")
                    .action(action)
                    .entityType("ORDER")
                    .entityId(event.getOrderId())
                    .message(message)
                    .data("{\"validationType\":\"" + event.getValidationType() + "\", \"sourceService\":\"" + event.getSourceService() + "\"}")
                    .severity(severity)
                    .build();

            notificationRepository.save(notification);
            log.info("Notificación de validación guardada: orderId={}, action={}", event.getOrderId(), action);
        } catch (Exception e) {
            log.error("Error procesando evento de validación: orderId={}", event.getOrderId(), e);
        }
    }
}
