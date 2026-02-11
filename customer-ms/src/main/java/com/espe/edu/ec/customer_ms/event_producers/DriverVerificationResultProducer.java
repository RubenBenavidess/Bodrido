package com.espe.edu.ec.customer_ms.event_producers;

import com.espe.edu.ec.customer_ms.config.RabbitMQConfig;
import com.espe.edu.ec.customer_ms.dtos.events.DriverValidationResultEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * Productor que publica resultados de validación de drivers hacia FleetService.
 * Es consultado por DriverValidationEventListener después de verificar un driver.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DriverVerificationResultProducer {
    
    private final RabbitTemplate rabbitTemplate;
    
    /**
     * Publica resultado de validación de driver a FleetService
     */
    public void publishVerificationResult(DriverValidationResultEvent result) {
        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.DRIVER_VALIDATION_RESULT_EXCHANGE,
                RabbitMQConfig.DRIVER_VALIDATION_RESULT_ROUTING_KEY,
                result
            );
            
            if (result.isValid()) {
                log.info("✓ [DRIVER-RESULT-PRODUCER] Resultado de validación EXITOSO publicado: driverId={}, userId={}", 
                    result.getDriverId(), result.getUserId());
            } else {
                log.warn("✗ [DRIVER-RESULT-PRODUCER] Resultado de validación FALLIDO publicado: driverId={}, userId={}, error={}", 
                    result.getDriverId(), result.getUserId(), result.getErrorMessage());
            }
        } catch (Exception e) {
            log.error("✗ [DRIVER-RESULT-PRODUCER] Error publicando resultado de validación: driverId={}, error={}", 
                result.getDriverId(), e.getMessage(), e);
            throw new RuntimeException("Error publicando resultado de validación de driver", e);
        }
    }
}
