package com.espe.edu.ec.customer_ms.mappers;

import com.espe.edu.ec.customer_ms.dtos.CustomerDTO;
import com.espe.edu.ec.customer_ms.dtos.UserCreatedEvent;
import com.espe.edu.ec.customer_ms.models.Customer;
import org.springframework.stereotype.Component;

@Component
public class CustomerMapper {
    
    public Customer toEntity(UserCreatedEvent event) {
        return Customer.builder()
                .userId(event.getUserId())
                .email(event.getEmail())
                .username(event.getUsername())
                .roleId(event.getRoleId())
                .vehicleType(event.getVehicleType())
                .zoneId(event.getZoneId())
                .isActive(true)
                .build();
    }
    
    public CustomerDTO toDTO(Customer customer) {
        return CustomerDTO.builder()
                .id(customer.getId())
                .userId(customer.getUserId())
                .email(customer.getEmail())
                .username(customer.getUsername())
                .isActive(customer.getIsActive())
                .roleId(customer.getRoleId())
                .vehicleType(customer.getVehicleType())
                .zoneId(customer.getZoneId())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }
}
