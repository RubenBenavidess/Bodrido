package com.espe.edu.ec.order_ms.mappers;

import com.espe.edu.ec.order_ms.dtos.OrderRequest;
import com.espe.edu.ec.order_ms.dtos.OrderResponse;
import com.espe.edu.ec.order_ms.models.Order;

public final class OrderMapper {

    private OrderMapper(){
        throw new UnsupportedOperationException("Clase utilitaria");
    };

    public static Order orderRequestToEntity(OrderRequest orderRequest){

        if(orderRequest == null) throw new IllegalArgumentException("Datos de pedido inválidos.");
        if(orderRequest.getCustomerId() == null) throw new IllegalArgumentException("Datos de pedido inválidos.");

        Order newOrder = Order.builder()
            .customerId(orderRequest.getCustomerId())
            .pickupAddress(orderRequest.getPickupAddress())
            .deliveryAddress(orderRequest.getDeliveryAddress())
            .build();

        newOrder.setItems(
            orderRequest.getItems()
                .stream()
                .map(OrderItemMapper::orderItemRequestToEntity)
                .toList() 
        );
            
        return newOrder;

    }

    public static OrderResponse entityToOrderResponse(Order order){
        if(order == null) return null;
        
        return OrderResponse.builder()
            .id(order.getId())
            .customerId(order.getCustomerId())
            .driverId(order.getDriverId())
            .vehicleId(order.getVehicleId())
            .status(order.getStatus())
            .distanceKm(order.getDistanceKm() != null ? 
                order.getDistanceKm().doubleValue() : null)
            .tripFee(order.getTripFee() != null ? 
                order.getTripFee().doubleValue() : null)
            .serviceFee(order.getServiceFee() != null ? 
                order.getServiceFee().doubleValue() : null)
            .totalAmount(order.getTotalAmount() != null ? 
                order.getTotalAmount().doubleValue() : null)
            .deliveryAddress(order.getDeliveryAddress())
            .pickupAddress(order.getPickupAddress())
            .orderItems(order.getItems() != null ?
                order.getItems().stream()
                    .map(OrderItemMapper::orderItemResponseToEntity)
                    .toList() : null)
            .orderDate(order.getCreatedAt())
            .build();
    }



}
