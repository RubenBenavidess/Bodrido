package com.espe.edu.ec.billing_ms.event_producers;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.espe.edu.ec.billing_ms.config.RabbitMQConfig;
import com.espe.edu.ec.billing_ms.models.Invoice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillingEventProducer {

    private final RabbitTemplate rabbitTemplate;

    private static final String ACTION_INVOICE_CREATED = "invoice_created";
    private static final String ACTION_INVOICE_ISSUED = "invoice_issued";
    private static final String SEVERITY_INFO = "INFO";

    /**
     * Publica evento cuando se crea una factura
     */
    public void publishInvoiceCreatedEvent(Invoice invoice) {
        Map<String, Object> data = new HashMap<>();
        data.put("orderId", invoice.getOrderId());
        data.put("invoiceId", invoice.getId());
        data.put("total", invoice.getTotal());
        data.put("status", invoice.getStatus().toString());
        
        sendEvent(invoice.getId(), ACTION_INVOICE_CREATED, "Factura creada exitosamente", data);
    }

    /**
     * Publica evento cuando se emite una factura
     */
    public void publishInvoiceIssuedEvent(Invoice invoice) {
        Map<String, Object> data = new HashMap<>();
        data.put("orderId", invoice.getOrderId());
        data.put("invoiceId", invoice.getId());
        data.put("total", invoice.getTotal());
        data.put("status", invoice.getStatus().toString());
        data.put("issuedAt", invoice.getIssuedAt());
        
        sendEvent(invoice.getId(), ACTION_INVOICE_ISSUED, "Factura emitida exitosamente", data);
    }

    /**
     * Envía evento de billing
     */
    private void sendEvent(UUID invoiceId, String action, String message, Map<String, Object> data) {
        BillingNotificationEvent event = BillingNotificationEvent.builder()
            .invoiceId(invoiceId)
            .action(action)
            .message(message)
            .timestamp(LocalDateTime.now())
            .data(data)
            .severity(SEVERITY_INFO)
            .build();
        
        try {
            String routingKey = "billing." + action;
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.BILLING_EXCHANGE_NAME,
                routingKey,
                event
            );
            log.info("Evento de billing publicado: invoiceId={}, action={}", invoiceId, action);
        } catch (Exception e) {
            log.error("Error al publicar evento de billing: invoiceId={}, action={}", invoiceId, action, e);
            throw new RuntimeException("Error al publicar evento de billing", e);
        }
    }
}
