package com.espe.edu.ec.customer_ms.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverCreationFailedEvent {
    
    @JsonProperty("user_id")
    private UUID userId;
    
    private String email;
    
    private String username;
    
    private String action;
    
    private String reason;
    
    private String message;
    
    private Long timestamp;
}
