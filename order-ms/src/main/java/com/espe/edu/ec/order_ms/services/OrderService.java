package com.espe.edu.ec.order_ms.services;

import java.util.List;
import java.util.UUID;

import com.espe.edu.ec.order_ms.dtos.OrderPatchRequest;
import com.espe.edu.ec.order_ms.dtos.OrderRequest;
import com.espe.edu.ec.order_ms.dtos.OrderResponse;

public interface OrderService {

    OrderResponse createOrder(OrderRequest orderRequest);
    OrderResponse getOrder(UUID id);
    List<OrderResponse> getOrders();
    OrderResponse patchOrder(UUID id, OrderPatchRequest orderPatchRequest);
    void cancelOrder(UUID id);

}
