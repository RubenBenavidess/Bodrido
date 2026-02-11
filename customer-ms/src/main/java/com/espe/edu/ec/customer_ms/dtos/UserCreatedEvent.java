package com.espe.edu.ec.customer_ms.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCreatedEvent {
    
    @JsonProperty("user_id")
    private UUID userId;
    
    private String email;
    
    private String username;
    
    @JsonProperty("role_id")
    private Integer roleId;
    
    @JsonProperty("vehicle_type")
    private String vehicleType;
    
    @JsonProperty("zone_id")
    private Integer zoneId;
    
    private String action;    
    private String message;    
    private Long timestamp;
}
