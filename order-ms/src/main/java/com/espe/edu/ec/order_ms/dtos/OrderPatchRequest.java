package com.espe.edu.ec.order_ms.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public abstract class OrderPatchRequest {

    private String instructions;
    private String reason;

}
