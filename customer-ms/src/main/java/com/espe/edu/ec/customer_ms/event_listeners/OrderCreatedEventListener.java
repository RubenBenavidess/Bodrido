package com.espe.edu.ec.customer_ms.event_listeners;

import com.espe.edu.ec.customer_ms.config.RabbitMQConfig;
import com.espe.edu.ec.customer_ms.dtos.OrderCreatedEvent;
import com.espe.edu.ec.customer_ms.services.OrderVerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreatedEventListener {
    
    private final OrderVerificationService orderVerificationService;
    
    @RabbitListener(queues = RabbitMQConfig.ORDERS_VALIDATIONS_CUSTOMER_QUEUE)
    public void handleOrderCreatedEvent(OrderCreatedEvent event) {
        try {
            log.info("► [ORDER-VERIFICATION] Evento recibido: Order ID = {}, Customer ID = {}", 
                event.getOrderId(), event.getCustomerId());
            
            // Verificar si el customer existe
            orderVerificationService.verifyCustomerAndPublishResult(event);
            
        } catch (Exception e) {
            log.error("✗ Error processando OrderCreatedEvent: {}", e.getMessage(), e);
            throw new RuntimeException("Error verifying customer", e);
        }
    }
}
