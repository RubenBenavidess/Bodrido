package com.espe.edu.ec.customer_ms.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderVerificationResultEvent {
    
    @JsonProperty("order_id")
    private UUID orderId;
    
    @JsonProperty("customer_id")
    private UUID customerId;
    
    @JsonProperty("verification_status")
    private String verificationStatus; // "SUCCESS" o "CUSTOMER_NOT_FOUND"
    
    private String reason;
    private Long timestamp;
}
