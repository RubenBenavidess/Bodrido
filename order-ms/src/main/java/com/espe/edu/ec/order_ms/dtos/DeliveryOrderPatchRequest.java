package com.espe.edu.ec.order_ms.dtos;

import com.espe.edu.ec.order_ms.models.Address;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class DeliveryOrderPatchRequest extends OrderPatchRequest{

    @NotNull(message = "Las coordenadas son obligatorias.")
    private Address.Coordinates newCoordinates;

}
