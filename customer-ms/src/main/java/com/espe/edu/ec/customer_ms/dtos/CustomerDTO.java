package com.espe.edu.ec.customer_ms.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerDTO {
    
    private UUID id;
    
    @JsonProperty("user_id")
    private UUID userId;
    
    private String email;
    
    private String username;
    
    @JsonProperty("is_active")
    private Boolean isActive;
    
    @JsonProperty("role_id")
    private Integer roleId;
    
    @JsonProperty("vehicle_type")
    private String vehicleType;
    
    @JsonProperty("zone_id")
    private Integer zoneId;
    
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
}
