package com.espe.edu.ec.order_ms.dtos.events;

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
    
    @JsonProperty("verification_status")
    private String verificationStatus;
    
    @JsonProperty("reason")
    private String reason;
    
    @JsonProperty("timestamp")
    private Long timestamp;
}
