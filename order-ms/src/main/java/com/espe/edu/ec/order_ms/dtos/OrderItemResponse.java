package com.espe.edu.ec.order_ms.dtos;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemResponse {

    private UUID id;
    private String description;
    private Integer quantity;
    private Double weightKg;
    private Double declaredValue;
    private Double handlingFee;

}
