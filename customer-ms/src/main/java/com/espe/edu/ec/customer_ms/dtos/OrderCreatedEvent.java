package com.espe.edu.ec.customer_ms.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCreatedEvent {
    
    @JsonProperty("order_id")
    private UUID orderId;
    
    @JsonProperty("customer_id")
    private UUID customerId;
    
    @JsonProperty("pickup_location")
    private Map<String, Object> pickupLocation;
    
    @JsonProperty("delivery_location")
    private Map<String, Object> deliveryLocation;
    
    @JsonProperty("total_price")
    private BigDecimal totalPrice;
    
    private String action;
    private String message;
    private Long timestamp;
}
