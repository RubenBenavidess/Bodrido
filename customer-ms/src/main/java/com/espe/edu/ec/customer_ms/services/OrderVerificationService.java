package com.espe.edu.ec.customer_ms.services;

import com.espe.edu.ec.customer_ms.dtos.CustomerVerificationResultEvent;
import com.espe.edu.ec.customer_ms.dtos.OrderCreatedEvent;
import com.espe.edu.ec.customer_ms.event_producers.CustomerVerificationResultProducer;
import com.espe.edu.ec.customer_ms.models.Customer;
import com.espe.edu.ec.customer_ms.repositories.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Servicio que verifica si un CLIENTE (no driver) existe y está activo.
 * Es consultado por order-ms cuando crea una orden.
 * 
 * ⚠️ IMPORTANTE: Solo verifica CLIENTs, excluye a DRIVERs (roleId != 2)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderVerificationService {
    
    private static final Integer DRIVER_ROLE_ID = 2;
    
    private final CustomerRepository customerRepository;
    private final CustomerVerificationResultProducer verificationResultProducer;
    
    /**
     * Verifica que un CLIENTE existe en customer-ms, está activo, y NO es un DRIVER
     */
    public void verifyCustomerAndPublishResult(OrderCreatedEvent event) {
        log.info("► [ORDER-VERIFICATION] Iniciando verificación de customer: {}", event.getCustomerId());
        
        // Buscar customer por ID
        Optional<Customer> customerOptional = customerRepository.findByUserId(event.getCustomerId());
        
        CustomerVerificationResultEvent result = CustomerVerificationResultEvent.builder()
            .orderId(event.getOrderId())
            .customerId(event.getCustomerId())
            .timestamp(System.currentTimeMillis())
            .action("customer_verification")
            .build();
        
        if (customerOptional.isEmpty()) {
            log.warn("✗ [ORDER-VERIFICATION] Customer NO ENCONTRADO: {}", event.getCustomerId());
            result.setCustomerExists(false);
            result.setVerificationStatus("NOT_FOUND");
            result.setReason("Customer does not exist in system");
            result.setMessage("Order will be cancelled - customer not found");
        } else {
            Customer customer = customerOptional.get();
            
            // Validar que NO sea un DRIVER (excluir roleId = 2)
            if (DRIVER_ROLE_ID.equals(customer.getRoleId())) {
                log.warn("✗ [ORDER-VERIFICATION] Usuario es DRIVER, no CLIENTE: {}, roleId={}", 
                    event.getCustomerId(), customer.getRoleId());
                result.setCustomerExists(false);
                result.setVerificationStatus("INVALID_ROLE");
                result.setReason("User has DRIVER role, not CLIENT");
                result.setMessage("Order will be cancelled - invalid customer type");
            } else if (!customer.getIsActive()) {
                log.warn("✗ [ORDER-VERIFICATION] Customer INACTIVO: {}", event.getCustomerId());
                result.setCustomerExists(true);
                result.setVerificationStatus("INACTIVE");
                result.setReason("Customer is inactive");
                result.setMessage("Order will be cancelled - customer inactive");
            } else {
                log.info("✓ [ORDER-VERIFICATION] Customer VERIFICADO: {}", event.getCustomerId());
                result.setCustomerExists(true);
                result.setVerificationStatus("VERIFIED");
                result.setReason("Customer verified and active");
                result.setMessage("Order verified - proceed with fulfillment");
            }
        }
        
        // Publicar resultado
        verificationResultProducer.publishVerificationResult(result);
    }
}
