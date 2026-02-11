package com.espe.edu.ec.customer_ms.event_listeners;

import com.espe.edu.ec.customer_ms.config.RabbitMQConfig;
import com.espe.edu.ec.customer_ms.dtos.events.DriverValidationEvent;
import com.espe.edu.ec.customer_ms.services.DriverVerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Listener que recibe solicitudes de validación de drivers desde FleetService.
 * 
 * Cuando FleetService intenta crear un conductor, primero valida
 * que el usuario exista en customer-ms y que tenga rol DRIVER.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DriverValidationEventListener {
    
    private final DriverVerificationService driverVerificationService;
    
    /**
     * Escucha eventos de validación de driver desde FleetService
     */
    @RabbitListener(queues = RabbitMQConfig.DRIVER_VALIDATION_QUEUE)
    public void handleDriverValidationEvent(DriverValidationEvent event) {
        try {
            log.info("► [DRIVER-VALIDATION-LISTENER] Evento recibido: Driver ID = {}, User ID = {}", 
                event.getDriverId(), event.getUserId());
            
            // Verificar que el driver existe y que tiene rol DRIVER
            driverVerificationService.verifyDriverAndPublishResult(event);
            
        } catch (Exception e) {
            log.error("✗ [DRIVER-VALIDATION-LISTENER] Error procesando DriverValidationEvent: {}", e.getMessage(), e);
            throw new RuntimeException("Error verificando driver", e);
        }
    }
}
