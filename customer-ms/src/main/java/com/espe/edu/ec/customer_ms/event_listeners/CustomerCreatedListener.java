package com.espe.edu.ec.customer_ms.event_listeners;

import com.espe.edu.ec.customer_ms.config.RabbitMQConfig;
import com.espe.edu.ec.customer_ms.dtos.UserCreatedEvent;
import com.espe.edu.ec.customer_ms.event_producers.UsersEventProducer;
import com.espe.edu.ec.customer_ms.services.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerCreatedListener {
    
    private final CustomerService customerService;
    private final UsersEventProducer eventProducer;
    
    @RabbitListener(queues = RabbitMQConfig.CUSTOMER_VALIDATION_QUEUE)
    @Transactional
    public void handleUserCreatedEvent(UserCreatedEvent event) {
        log.info("Evento de usuario creado recibido: userId={}, email={}, action={}", 
                 event.getUserId(), event.getEmail(), event.getAction());
        
        try {
            // Validar que el evento tenga los datos requeridos
            if (event.getUserId() == null) {
                log.warn("Evento sin userId, rechazando");
                throw new IllegalArgumentException("userId es requerido en el evento");
            }
            
            // Crear el cliente basado en el evento
            var customerDTO = customerService.createCustomerFromEvent(event);
            
            log.info("Cliente creado exitosamente desde evento: customerId={}, userId={}", 
                     customerDTO.getId(), customerDTO.getUserId());
            
        } catch (IllegalArgumentException e) {
            log.warn("Cliente ya existe, ignorando duplicado: userId={}", event.getUserId());
            // No relanzar excepción para evitar que se envíe a DLQ
        } catch (Exception e) {
            log.error("Error procesando evento de usuario: userId={}, error={}", event.getUserId(), e.getMessage(), e);
            // Relanzar para que se envíe a Dead Letter Queue y reintente
            throw new RuntimeException("Error procesando evento de usuario", e);
        }
    }
    
    /**
     * Maneja eventos no procesables que regresan de la Dead Letter Queue
     * SAGA PATTERN - COMPENSACIÓN: Publica evento de fallo para que Auth-ms desactive al usuario
     */
    @RabbitListener(queues = RabbitMQConfig.CUSTOMER_VALIDATION_DLQ)
    public void handleFailedCustomerEvent(UserCreatedEvent event) {
        log.error("⚠️ Evento en Dead Letter Queue - Iniciando compensación: userId={}, email={}", 
                  event.getUserId(), event.getEmail());
        
        try {
            // Publicar evento de fallo para que Auth-ms desactive el usuario
            eventProducer.publishCustomerCreationFailedEvent(
                    event,
                    "Fallo permanente en customer-ms después de múltiples reintentos"
            );
            
            log.error("✓ Evento de compensación publicado. Auth-ms desactivará el usuario: userId={}", 
                      event.getUserId());
        } catch (Exception e) {
            log.error("✗ Error publicando compensación: userId={}, error={}", 
                     event.getUserId(), e.getMessage(), e);
        }
    }
}
