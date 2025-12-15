package com.espe.edu.ec.order_ms.dtos;

import java.time.LocalDateTime;
import java.util.UUID;

import com.espe.edu.ec.order_ms.model_enums.OrderStatus;
import com.espe.edu.ec.order_ms.models.Address;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse{

    private UUID id;
    private UUID customerId;
    private UUID driverId;
    private String vehicleId;
    private OrderStatus status;
    private Double distanceKm;
    private Double tripFee;
    private Double serviceFee;
    private Double totalAmount;
    private Address deliveryAddress;
    private Address pickupAddress;
    private List<OrderItemResponse> orderItems;
    private LocalDateTime orderDate;

}