package com.espe.edu.ec.order_ms.dtos;

import com.espe.edu.ec.order_ms.model_enums.VehicleType;
import com.espe.edu.ec.order_ms.models.Address;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderRequest {

    private UUID customerId;
    private VehicleType vehicleType;
    private Address pickupAddress;
    private Address deliveryAddress;
    private List<OrderItemRequest> items;

}
