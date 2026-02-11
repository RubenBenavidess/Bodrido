package com.espe.edu.ec.customer_ms.event_producers;

import com.espe.edu.ec.customer_ms.config.RabbitMQConfig;
import com.espe.edu.ec.customer_ms.dtos.CustomerVerificationResultEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerVerificationResultProducer {
    
    private final RabbitTemplate rabbitTemplate;
    
    public void publishVerificationResult(CustomerVerificationResultEvent result) {
        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_VERIFICATION_RESULT_EXCHANGE,
                RabbitMQConfig.ORDER_VERIFICATION_RESULT_ROUTING_KEY,
                result
            );
            
            log.info("✓ Resultado de verificación publicado: Order ID = {}, Status = {}", 
                result.getOrderId(), result.getVerificationStatus());
                
        } catch (Exception e) {
            log.error("✗ Error publicando resultado de verificación: {}", e.getMessage(), e);
            throw new RuntimeException("Error publishing verification result", e);
        }
    }
}
