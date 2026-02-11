package com.espe.edu.ec.customer_ms.event_producers;

import com.espe.edu.ec.customer_ms.config.RabbitMQConfig;
import com.espe.edu.ec.customer_ms.dtos.CustomerCreationFailedEvent;
import com.espe.edu.ec.customer_ms.dtos.DriverCreationFailedEvent;
import com.espe.edu.ec.customer_ms.dtos.UserCreatedEvent;
import com.espe.edu.ec.customer_ms.dtos.events.DriverUserCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsersEventProducer {
    
    private final RabbitTemplate rabbitTemplate;
    
    /**
     * Publica evento de fallo de creación de cliente para que Auth-ms desactive el usuario
     */
    public void publishCustomerCreationFailedEvent(UserCreatedEvent originalEvent, String reason) {
        try {
            CustomerCreationFailedEvent failureEvent = CustomerCreationFailedEvent.builder()
                    .userId(originalEvent.getUserId())
                    .email(originalEvent.getEmail())
                    .username(originalEvent.getUsername())
                    .action("customer_creation_failed")
                    .reason(reason)
                    .message("Fallo al crear registro de cliente en customer-ms. Usuario debe ser desactivado.")
                    .timestamp(System.currentTimeMillis())
                    .build();
            
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.ROLLBACK_EXCHANGE, 
                RabbitMQConfig.ROLLBACK_ROUTING_KEY, 
                failureEvent
            );
            
            log.error("Evento de fallo de cliente publicado para compensación: userId={}, reason={}", 
                     originalEvent.getUserId(), reason);
        } catch (Exception e) {
            log.error("Error publicando evento de compensación: userId={}, error={}", 
                     originalEvent.getUserId(), e.getMessage(), e);
        }
    }
    
    /**
     * Publica evento de fallo de creación de driver para que Auth-ms desactive el usuario
     * SAGA PATTERN - COMPENSACIÓN: Notifica a auth-ms que el driver no pudo ser registrado
     */
    public void publishDriverCreationFailedEvent(DriverUserCreatedEvent originalEvent, String reason) {
        try {
            DriverCreationFailedEvent failureEvent = DriverCreationFailedEvent.builder()
                    .userId(originalEvent.getUserId())
                    .email(originalEvent.getEmail())
                    .username(originalEvent.getUsername())
                    .action("driver_creation_failed")
                    .reason(reason)
                    .message("Fallo al registrar usuario driver en customer-ms. Usuario debe ser desactivado.")
                    .timestamp(System.currentTimeMillis())
                    .build();
            
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.ROLLBACK_EXCHANGE, 
                RabbitMQConfig.ROLLBACK_ROUTING_KEY, 
                failureEvent
            );
            
            log.error("✗ Evento de fallo de driver publicado para compensación: userId={}, reason={}", 
                     originalEvent.getUserId(), reason);
        } catch (Exception e) {
            log.error("✗ Error publicando evento de compensación del driver: userId={}, error={}", 
                     originalEvent.getUserId(), e.getMessage(), e);
        }
    }
}
