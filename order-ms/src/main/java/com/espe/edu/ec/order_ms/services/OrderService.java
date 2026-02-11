package com.espe.edu.ec.order_ms.services;

import java.util.List;
import java.util.UUID;

import com.espe.edu.ec.order_ms.dtos.AssignDriverRequest;
import com.espe.edu.ec.order_ms.dtos.OrderPatchRequest;
import com.espe.edu.ec.order_ms.dtos.OrderRequest;
import com.espe.edu.ec.order_ms.dtos.OrderResponse;

public interface OrderService {

    OrderResponse createOrder(OrderRequest orderRequest);

    OrderResponse getOrder(UUID id);

    List<OrderResponse> getOrders();

    boolean orderExists(UUID id);

    OrderResponse patchOrder(UUID id, OrderPatchRequest orderPatchRequest);

    void cancelOrder(UUID id);

    OrderResponse pickupOrder(UUID id);

    List<OrderResponse> getOrdersByCustomer(UUID customerId);

    OrderResponse assignDriverAndVehicle(UUID orderId, AssignDriverRequest request);

    void cancelOrderDueToVerificationFailure(UUID orderId, String reason);

    void confirmOrderVerification(UUID orderId);

    void dropOrderDueToRejection(UUID orderId, String reason);

    void dropOrderDueToTimeout(UUID orderId);

    void confirmAssignmentVerification(UUID orderId);

    void rejectAssignmentAndReturnToCreated(UUID orderId, String reason);

    // === Saga de cancelación ===
    void confirmCancellation(UUID orderId);

    void rejectCancellationAndRestore(UUID orderId, String reason);

    void cancelOrderDueToCancellationTimeout(UUID orderId);

    // === Saga de pickup ===
    void confirmPickup(UUID orderId);

    void rejectPickupAndReturnToAssigned(UUID orderId, String reason);

    void rejectPickupDueToTimeout(UUID orderId);

}
