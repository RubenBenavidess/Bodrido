package com.espe.edu.ec.customer_ms.services;

import com.espe.edu.ec.customer_ms.dtos.events.DriverValidationEvent;
import com.espe.edu.ec.customer_ms.dtos.events.DriverValidationResultEvent;
import com.espe.edu.ec.customer_ms.event_producers.DriverVerificationResultProducer;
import com.espe.edu.ec.customer_ms.models.Customer;
import com.espe.edu.ec.customer_ms.repositories.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Servicio que verifica si un usuario existe y tiene rol DRIVER.
 * Es consultado por FleetService cuando intenta crear un conductor.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DriverVerificationService {
    
    private static final Integer DRIVER_ROLE_ID = 2;
    
    private final CustomerRepository customerRepository;
    private final DriverVerificationResultProducer verificationResultProducer;
    
    /**
     * Verifica que un usuario exista en customer-ms AND que tenga rol DRIVER (roleId = 2)
     * Publica el resultado a FleetService
     */
    public void verifyDriverAndPublishResult(DriverValidationEvent event) {
        log.info("👮 [DRIVER-VERIFICATION] Verificando existencia de driver: userId={}, driverId={}", 
            event.getUserId(), event.getDriverId());
        
        // Buscar customer por userId
        Optional<Customer> customerOptional = customerRepository.findByUserId(event.getUserId());
        
        DriverValidationResultEvent result = DriverValidationResultEvent.builder()
            .driverId(event.getDriverId())
            .userId(event.getUserId())
            .timestamp(System.currentTimeMillis())
            .action("driver_validation_result")
            .build();
        
        if (customerOptional.isEmpty()) {
            log.warn("✗ [DRIVER-VERIFICATION] Usuario NO ENCONTRADO en customer-ms: userId={}", 
                event.getUserId());
            result.setValid(false);
            result.setErrorMessage("Usuario no existe en customer-ms");
        } else {
            Customer customer = customerOptional.get();
            
            // Validar que sea un driver (roleId = 2)
            if (!DRIVER_ROLE_ID.equals(customer.getRoleId())) {
                log.warn("✗ [DRIVER-VERIFICATION] Usuario NO ES DRIVER (roleId={}): userId={}", 
                    customer.getRoleId(), event.getUserId());
                result.setValid(false);
                result.setErrorMessage("Usuario existe pero no tiene rol DRIVER. RoleId: " + customer.getRoleId());
            } else if (!customer.getIsActive()) {
                log.warn("✗ [DRIVER-VERIFICATION] Usuario INACTIVO: userId={}", event.getUserId());
                result.setValid(false);
                result.setErrorMessage("Usuario driver existe pero está inactivo");
            } else {
                log.info("✓ [DRIVER-VERIFICATION] Driver VALIDADO: userId={}, driverId={}", 
                    event.getUserId(), event.getDriverId());
                result.setValid(true);
            }
        }
        
        // Publicar resultado a FleetService
        verificationResultProducer.publishVerificationResult(result);
    }
    
    /**
     * Verifica simplemente si un usuario con rol DRIVER existe
     */
    public boolean driverExists(java.util.UUID userId) {
        return customerRepository.findByUserId(userId)
            .map(customer -> DRIVER_ROLE_ID.equals(customer.getRoleId()) && customer.getIsActive())
            .orElse(false);
    }
}
