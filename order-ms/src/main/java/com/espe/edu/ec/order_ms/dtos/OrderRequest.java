package com.espe.edu.ec.order_ms.dtos;

import com.espe.edu.ec.order_ms.model_enums.VehicleType;
import com.espe.edu.ec.order_ms.models.Address;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

import org.hibernate.validator.constraints.Length;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderRequest {

    @NotNull(message = "El ID del cliente es obligatorio.")
    private UUID customerId;

    @NotNull(message = "El tipo de vehículo es obligatorio.")
    private VehicleType vehicleType;

    @Valid
    private Address pickupAddress;
    @Valid
    private Address deliveryAddress;

    @NotNull(message = "La lista de items es obligatoria.")
    @NotEmpty(message = "La lista de items debe contener al menos un elemento.")
    @Valid
    private List<OrderItemRequest> items;

}
