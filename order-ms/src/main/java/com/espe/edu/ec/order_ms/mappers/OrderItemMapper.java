package com.espe.edu.ec.order_ms.mappers;

import com.espe.edu.ec.order_ms.dtos.OrderItemRequest;
import com.espe.edu.ec.order_ms.dtos.OrderItemResponse;
import com.espe.edu.ec.order_ms.models.OrderItem;

public final class OrderItemMapper {

    private OrderItemMapper(){
        throw new UnsupportedOperationException("Clase utilitaria");
    };

    public static OrderItem orderItemRequestToEntity(OrderItemRequest orderItemRequest){
        
        if(orderItemRequest == null) throw new IllegalArgumentException("Datos de item de pedido inválidos.");

        return OrderItem.builder()
            .description(orderItemRequest.getDescription())
            .quantity(orderItemRequest.getQuantity())
            .weightKg(orderItemRequest.getWeightKg() != null ? 
                new java.math.BigDecimal(orderItemRequest.getWeightKg()) : null)
            .build();
    }

    public static OrderItemResponse orderItemResponseToEntity(OrderItem orderItem){
        
        if(orderItem == null) throw new IllegalArgumentException("Datos de item de pedido inválidos.");
        
        return OrderItemResponse.builder()
            .id(orderItem.getId())
            .description(orderItem.getDescription())
            .quantity(orderItem.getQuantity())
            .weightKg(orderItem.getWeightKg() != null ? 
                orderItem.getWeightKg().doubleValue() : null)
            .declaredValue(orderItem.getDeclaredValue() != null ? 
                orderItem.getDeclaredValue().doubleValue() : null)
            .handlingFee(orderItem.getHandlingFee() != null ? 
                orderItem.getHandlingFee().doubleValue() : null)
            .build();
    }

}

