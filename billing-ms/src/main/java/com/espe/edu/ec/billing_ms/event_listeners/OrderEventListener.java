package com.espe.edu.ec.billing_ms.event_listeners;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.espe.edu.ec.billing_ms.config.RabbitMQConfig;
import com.espe.edu.ec.billing_ms.dtos.InvoiceRequest;
import com.espe.edu.ec.billing_ms.event_producers.OrderNotificationEvent;
import com.espe.edu.ec.billing_ms.services.InvoiceService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {

    private final InvoiceService invoiceService;

    /**
     * Escucha eventos de creación de órdenes desde OrderMS
     * y automáticamente crea un invoice en estado DRAFT
     */
    @RabbitListener(queues = RabbitMQConfig.BILLING_QUEUE_NAME)
    @Transactional
    public void handleOrderCreatedEvent(OrderNotificationEvent event) {
        log.info("Evento de orden recibido: orderId={}, action={}", event.getOrderId(), event.getAction());

        if (event.getOrderId() == null) {
            log.warn("Evento sin orderId, ignorando");
            return;
        }

        try {
            if ("create".equals(event.getAction())) {
                handleOrderCreated(event);
            }
        } catch (Exception e) {
            log.error("Error procesando evento de orden: orderId={}, action={}", 
                event.getOrderId(), event.getAction(), e);
        }
    }

    /**
     * Maneja la creación automática de invoice cuando se crea una orden
     */
    private void handleOrderCreated(OrderNotificationEvent event) {
        UUID orderId = event.getOrderId();
        
        // Verificar que no exista ya un invoice para esta orden
        try {
            invoiceService.getInvoiceByOrderId(orderId);
            log.warn("Ya existe un invoice para la orden: {}", orderId);
            return;
        } catch (IllegalArgumentException e) {
            // Es normal que no exista, continuamos
            log.debug("No existe invoice para orden {}, procederemos a crear uno", orderId);
        }

        // Crear invoice automáticamente
        try {
            // Extraer datos del evento
            Map<String, Object> data = event.getData();
            if (data == null) {
                log.warn("Evento de orden sin datos para orderId: {}", orderId);
                return;
            }

            String customerTaxId = (String) data.get("customerTaxId");
            if (customerTaxId == null || customerTaxId.isBlank()) {
                log.warn("CustomerTaxId no disponible en evento para orderId: {}", orderId);
                return;
            }

            BigDecimal total = new BigDecimal(data.get("totalAmount").toString());
            
            // Calcular subtotal y tax (IVA 12%)
            BigDecimal subtotal = total.divide(new BigDecimal("1.12"), 2, java.math.RoundingMode.HALF_UP);
            BigDecimal taxAmount = total.subtract(subtotal);

            InvoiceRequest invoiceRequest = new InvoiceRequest();
            invoiceRequest.setOrderId(orderId);
            invoiceRequest.setCustomerTaxId(customerTaxId);
            invoiceRequest.setSubtotal(subtotal);
            invoiceRequest.setTaxAmount(taxAmount);
            invoiceRequest.setTotal(total);

            invoiceService.createInvoice(invoiceRequest);
            
            log.info("Invoice creado automáticamente para orden: {}", orderId);
        } catch (Exception e) {
            log.error("Error creando invoice automático para orden {}", orderId, e);
        }
    }
}
