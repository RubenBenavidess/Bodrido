package com.espe.edu.ec.customer_ms.dtos.events;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DriverUserCreatedEvent {
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
    
    private long timestamp;
}
