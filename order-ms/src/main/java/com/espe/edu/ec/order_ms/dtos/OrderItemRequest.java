package com.espe.edu.ec.order_ms.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemRequest {

    private String description;

    @Positive(message = "La cantidad de un item debe ser un número positivo.")
    @NotNull(message = "La cantidad de un item es obligatoria.")
    private Integer quantity;

    @DecimalMin(value = "0.01", inclusive = true)
    @NotNull(message = "El peso de un item es obligatorio.")
    private Double weightKg;

}
