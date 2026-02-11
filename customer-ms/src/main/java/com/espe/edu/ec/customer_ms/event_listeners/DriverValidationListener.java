package com.espe.edu.ec.customer_ms.event_listeners;

import com.espe.edu.ec.customer_ms.config.RabbitMQConfig;
import com.espe.edu.ec.customer_ms.dtos.events.DriverValidationEvent;
import com.espe.edu.ec.customer_ms.dtos.events.DriverValidationResultEvent;
import com.espe.edu.ec.customer_ms.models.Customer;
import com.espe.edu.ec.customer_ms.repositories.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Listener que valida conductores desde FleetService.
 * 
 * Cuando FleetService crea un conductor, valida:
 * 1. Que el userId existe en customer-ms
 * 2. Que el usuario tiene rol DRIVER (roleId = 2)
 * 3. Que el usuario está activo
 * 
 * Responde con DriverValidationResultEvent indicando éxito o fallo.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DriverValidationListener {

    private static final int ROLE_ID_DRIVER = 2; // Corresponde a "DRIVER" en auth-ms

    private final CustomerRepository customerRepository;
    private final RabbitTemplate rabbitTemplate;

    /**
     * Escucha eventos de validación de drivers desde FleetService.
     * Valida que el usuario existe, tiene rol DRIVER y está activo.
     */
    @RabbitListener(queues = RabbitMQConfig.DRIVER_VALIDATION_QUEUE)
    public void handleDriverValidation(DriverValidationEvent event) {
        log.info("👤 [DRIVER-VALIDATION] Evento recibido: DriverId={}, UserId={}", 
            event.getDriverId(), event.getUserId());

        boolean isValid = false;
        String errorMessage = null;

        try {
            // 1. Buscar el usuario en la BD (tabla customers)
            Optional<Customer> customerOpt = customerRepository.findByUserId(event.getUserId());
            
            if (customerOpt.isEmpty()) {
                isValid = false;
                errorMessage = "Usuario no encontrado: " + event.getUserId();
                log.warn("✗ [DRIVER-VALIDATION] Usuario no encontrado: {}", event.getUserId());
            } else {
                Customer customer = customerOpt.get();
                
                // 2. Verificar que el usuario tiene rol DRIVER (roleId = 2)
                if (customer.getRoleId() == null || !customer.getRoleId().equals(ROLE_ID_DRIVER)) {
                    isValid = false;
                    errorMessage = "Usuario no tiene rol DRIVER. RoleId actual: " + 
                        (customer.getRoleId() != null ? customer.getRoleId() : "NONE");
                    log.warn("✗ [DRIVER-VALIDATION] Usuario {} no tiene rol DRIVER (roleId=2)", event.getUserId());
                } 
                // 3. Verificar que el usuario está activo
                else if (!customer.getIsActive()) {
                    isValid = false;
                    errorMessage = "Usuario inactivo";
                    log.warn("✗ [DRIVER-VALIDATION] Usuario {} está inactivo", event.getUserId());
                } 
                else {
                    // ¡Todo bien!
                    isValid = true;
                    log.info("✓ [DRIVER-VALIDATION] Usuario validado correctamente: {}", event.getUserId());
                }
            }
        } catch (Exception e) {
            isValid = false;
            errorMessage = "Error validando driver: " + e.getMessage();
            log.error("✗ [DRIVER-VALIDATION] Error en validación", e);
        }

        // Publicar resultado de vuelta a FleetService
        publishValidationResult(event.getDriverId(), event.getUserId(), isValid, errorMessage);
    }

    /**
     * Publica el resultado de la validación de driver a FleetService.
     */
    private void publishValidationResult(java.util.UUID driverId, java.util.UUID userId, 
                                        boolean isValid, String errorMessage) {
        try {
            DriverValidationResultEvent result = DriverValidationResultEvent.builder()
                .driverId(driverId)
                .userId(userId)
                .isValid(isValid)
                .errorMessage(errorMessage)
                .timestamp(System.currentTimeMillis())
                .build();

            rabbitTemplate.convertAndSend(
                RabbitMQConfig.DRIVER_VALIDATION_RESULT_EXCHANGE,
                RabbitMQConfig.DRIVER_VALIDATION_RESULT_ROUTING_KEY,
                result
            );

            log.info("✓ [DRIVER-VALIDATION] Resultado publicado: DriverId={}, IsValid={}, Error={}", 
                driverId, isValid, errorMessage != null ? errorMessage : "None");
        } catch (Exception e) {
            log.error("✗ [DRIVER-VALIDATION] Error publicando resultado: DriverId={}", driverId, e);
        }
    }
}
