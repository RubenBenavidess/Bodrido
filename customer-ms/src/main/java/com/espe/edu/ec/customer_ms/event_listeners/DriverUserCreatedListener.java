package com.espe.edu.ec.customer_ms.event_listeners;

import com.espe.edu.ec.customer_ms.config.RabbitMQConfig;
import com.espe.edu.ec.customer_ms.dtos.events.DriverUserCreatedEvent;
import com.espe.edu.ec.customer_ms.event_producers.UsersEventProducer;
import com.espe.edu.ec.customer_ms.models.Customer;
import com.espe.edu.ec.customer_ms.repositories.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Listener que registra nuevos usuarios con rol DRIVER cuando se crean en auth-ms.
 * 
 * Cuando auth-ms publica que se creó un usuario DRIVER,
 * customer-ms lo registra en la tabla customers para que pueda ser validado
 * posteriormente cuando FleetService cree el conductor.
 * 
 * SAGA PATTERN: Implementa compensación automática en caso de fallo.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DriverUserCreatedListener {

    private final CustomerRepository customerRepository;
    private final UsersEventProducer eventProducer;

    /**
     * Escucha eventos de creación de usuarios DRIVER desde auth-ms.
     * Registra el usuario en la tabla customers si no existe.
     */
    @RabbitListener(queues = RabbitMQConfig.DRIVER_USER_CREATED_QUEUE)
    @Transactional 
    public void handleDriverUserCreated(DriverUserCreatedEvent event) {
        log.info("👤 [DRIVER-USER-CREATED] Nuevo usuario driver recibido desde auth-ms: UserId={}, Email={}", 
            event.getUserId(), event.getEmail());

        try {
            // Verificar si el usuario ya existe en customer-ms
            if (customerRepository.findByUserId(event.getUserId()).isPresent()) {
                log.warn("⚠️ [DRIVER-USER-CREATED] Usuario ya existe en customer-ms: UserId={}", event.getUserId());
                return;
            }

            // Crear el Customer basado en el evento
            Customer newCustomer = Customer.builder()
                .userId(event.getUserId())
                .email(event.getEmail())
                .username(event.getUsername())
                .roleId(event.getRoleId())  // roleId = 2 para DRIVER
                .vehicleType(event.getVehicleType())
                .zoneId(event.getZoneId())
                .isActive(true)
                .build();

            customerRepository.save(newCustomer);
            log.info("✓ [DRIVER-USER-CREATED] Usuario driver registrado en customer-ms: UserId={}, Email={}", 
                event.getUserId(), event.getEmail());

        } catch (Exception e) {
            log.error("✗ [DRIVER-USER-CREATED] Error registrando usuario driver: UserId={}, Error={}", 
                event.getUserId(), e.getMessage(), e);
            // Relanzar excepción para que vaya a Dead Letter Queue
            throw new RuntimeException("Error registrando usuario driver en customer-ms", e);
        }
    }
    
    /**
     * Maneja eventos no procesables que regresan de la Dead Letter Queue
     * SAGA PATTERN - COMPENSACIÓN: Publica evento de fallo para que Auth-ms desactive al usuario
     */
    @RabbitListener(queues = RabbitMQConfig.DRIVER_USER_CREATED_DLQ)
    public void handleFailedDriverEvent(DriverUserCreatedEvent event) {
        log.error("⚠️ [DRIVER-USER-CREATED-DLQ] Evento en Dead Letter Queue - Iniciando compensación: userId={}, email={}", 
                  event.getUserId(), event.getEmail());
        
        try {
            // Publicar evento de fallo para que Auth-ms desactive el usuario
            eventProducer.publishDriverCreationFailedEvent(
                    event,
                    "Fallo permanente en customer-ms después de múltiples reintentos"
            );
            
            log.error("✓ [DRIVER-USER-CREATED-DLQ] Evento de compensación publicado. Auth-ms desactivará el usuario: userId={}", 
                      event.getUserId());
        } catch (Exception e) {
            log.error("✗ [DRIVER-USER-CREATED-DLQ] Error publicando compensación: userId={}, error={}", 
                     event.getUserId(), e.getMessage(), e);
        }
    }
}
