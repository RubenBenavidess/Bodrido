package com.espe.edu.ec.customer_ms.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerVerificationResultEvent {
    
    @JsonProperty("order_id")
    private UUID orderId;
    
    @JsonProperty("customer_id")
    private UUID customerId;
    
    @JsonProperty("customer_exists")
    private Boolean customerExists;
    
    @JsonProperty("verification_status")
    private String verificationStatus; // "VERIFIED", "NOT_FOUND", "INACTIVE"
    
    private String reason;
    private String action;
    private String message;
    private Long timestamp;
}
