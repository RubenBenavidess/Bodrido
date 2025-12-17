package com.espe.edu.ec.order_ms.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class AssignDriverRequest {

    @NotNull(message = "El ID del conductor es obligatorio")
    private UUID driverId;

    @NotBlank(message = "El ID del vehículo es obligatorio")
    private String vehicleId;
}