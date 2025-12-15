package com.espe.edu.ec.order_ms.dtos;

import com.espe.edu.ec.order_ms.models.Address;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class DeliveryOrderPatchRequest extends OrderPatchRequest{

    private Address.Coordinates newCoordinates;

}
