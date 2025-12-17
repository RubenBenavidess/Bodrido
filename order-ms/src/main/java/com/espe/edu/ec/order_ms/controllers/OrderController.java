package com.espe.edu.ec.order_ms.controllers;

import com.espe.edu.ec.order_ms.dtos.DeliveryOrderPatchRequest;
import com.espe.edu.ec.order_ms.dtos.OrderRequest;
import com.espe.edu.ec.order_ms.dtos.OrderResponse;
import com.espe.edu.ec.order_ms.services.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody @Valid OrderRequest orderRequest) {
        OrderResponse response = orderService.createOrder(orderRequest);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable UUID id) {
        OrderResponse response = orderService.getOrder(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        List<OrderResponse> orders = orderService.getOrders();
        
        if (orders.isEmpty()) return ResponseEntity.noContent().build(); 
        
        return ResponseEntity.ok(orders); 
    }

    @PatchMapping("/{id}")
    public ResponseEntity<OrderResponse> patchOrder(
            @PathVariable UUID id, 
            @RequestBody DeliveryOrderPatchRequest patchRequest) {
        OrderResponse response = orderService.patchOrder(id, patchRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelOrder(@PathVariable UUID id) {
        orderService.cancelOrder(id);
        return ResponseEntity.noContent().build();
    }
}