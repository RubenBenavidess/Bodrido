package com.espe.edu.ec.customer_ms.services;

import com.espe.edu.ec.customer_ms.dtos.CustomerDTO;
import com.espe.edu.ec.customer_ms.dtos.UserCreatedEvent;
import com.espe.edu.ec.customer_ms.mappers.CustomerMapper;
import com.espe.edu.ec.customer_ms.models.Customer;
import com.espe.edu.ec.customer_ms.repositories.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {
    
    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;
    
    @Transactional
    public CustomerDTO createCustomerFromEvent(UserCreatedEvent event) {
        log.info("Creando cliente desde evento: userId={}, email={}", event.getUserId(), event.getEmail());
        
        // Verificar si el cliente ya existe
        if (customerRepository.findByUserId(event.getUserId()).isPresent()) {
            log.warn("Cliente ya existe para userId: {}", event.getUserId());
            throw new IllegalArgumentException("Cliente ya existe para este usuario");
        }
        
        // Mapear evento a entidad
        Customer customer = customerMapper.toEntity(event);
        
        // Guardar en BD
        Customer savedCustomer = customerRepository.save(customer);
        log.info("Cliente guardado exitosamente: customerId={}, userId={}", savedCustomer.getId(), savedCustomer.getUserId());
        
        return customerMapper.toDTO(savedCustomer);
    }
    
    @Transactional(readOnly = true)
    public CustomerDTO getCustomerByUserId(UUID userId) {
        log.info("Buscando cliente por userId: {}", userId);
        
        Customer customer = customerRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.warn("Cliente no encontrado para userId: {}", userId);
                    return new RuntimeException("Cliente no encontrado");
                });
        
        return customerMapper.toDTO(customer);
    }
    
    @Transactional(readOnly = true)
    public CustomerDTO getCustomerById(UUID id) {
        log.info("Buscando cliente por id: {}", id);
        
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Cliente no encontrado para id: {}", id);
                    return new RuntimeException("Cliente no encontrado");
                });
        
        return customerMapper.toDTO(customer);
    }
    
    @Transactional
    public CustomerDTO updateCustomerStatus(UUID userId, Boolean isActive) {
        log.info("Actualizando estado del cliente: userId={}, isActive={}", userId, isActive);
        
        Customer customer = customerRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        
        customer.setIsActive(isActive);
        Customer updated = customerRepository.save(customer);
        
        log.info("Cliente actualizado: userId={}, isActive={}", userId, isActive);
        return customerMapper.toDTO(updated);
    }
    
    @Transactional(readOnly = true)
    public boolean customerExists(UUID userId) {
        return customerRepository.findByUserId(userId).isPresent();
    }
}
