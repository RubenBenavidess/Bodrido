package com.espe.edu.ec.order_ms.event_listeners;

import com.espe.edu.ec.order_ms.config.RabbitMQConfig;
import com.espe.edu.ec.order_ms.dtos.events.CustomerVerificationResultEvent;
import com.espe.edu.ec.order_ms.services.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerVerificationResultListener {
    
    private final OrderService orderService;
    
    @RabbitListener(queues = RabbitMQConfig.ORDER_VERIFICATION_RESULT_QUEUE)
    public void handleVerificationResult(CustomerVerificationResultEvent event) {
        try {
            log.info("► [VERIFICATION-RESULT] Resultado recibido: Order ID = {}, Status = {}", 
                event.getOrderId(), event.getVerificationStatus());
            
            if ("VERIFIED".equals(event.getVerificationStatus()) || "SUCCESS".equals(event.getVerificationStatus())) {
                log.info("✓ [VERIFICATION-RESULT] Customer VERIFICADO - Confirmando orden: {}", event.getOrderId());
                orderService.confirmOrderVerification(event.getOrderId());
                
            } else {
                // NOT_FOUND o INACTIVE - Descartar la orden (DROP)
                log.warn("✗ [VERIFICATION-RESULT] Customer verification FAILED - Descartando orden: {}", 
                    event.getOrderId());
                    
                String reason = "Customer verification failed: " + event.getReason();
                orderService.dropOrderDueToRejection(event.getOrderId(), reason);
            }
            
        } catch (Exception e) {
            log.error("✗ Error procesando resultado de verificación: {}", e.getMessage(), e);
            throw new RuntimeException("Error processing verification result", e);
        }
    }
}
