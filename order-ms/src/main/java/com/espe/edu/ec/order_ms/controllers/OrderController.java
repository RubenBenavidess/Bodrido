package com.espe.edu.ec.order_ms.controllers;

import com.espe.edu.ec.order_ms.dtos.DeliveryOrderPatchRequest;
import com.espe.edu.ec.order_ms.dtos.OrderRequest;
import com.espe.edu.ec.order_ms.dtos.OrderResponse;
import com.espe.edu.ec.order_ms.services.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Tag(name = "Pedidos", description = "Operaciones de pedidos protegidas con JWT")
@SecurityRequirement(name = "bearerAuth") // Candado en Swagger
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "Crear pedido", description = "Requiere scope: order:create")
    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_order:create')")
    public ResponseEntity<OrderResponse> createOrder(@RequestBody @Valid OrderRequest orderRequest) {
        OrderResponse response = orderService.createOrder(orderRequest);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Obtener pedido", description = "Requiere scope: order:view")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_order:view')")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable UUID id) {
        OrderResponse response = orderService.getOrder(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Listar pedidos", description = "Requiere scope: order:view")
    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_order:view')")
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        List<OrderResponse> orders = orderService.getOrders();
        if (orders.isEmpty()) return ResponseEntity.noContent().build(); 
        return ResponseEntity.ok(orders); 
    }

    @Operation(summary = "Actualizar pedido", description = "Requiere scope: order:update")
    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_order:update')")
    public ResponseEntity<OrderResponse> patchOrder(
            @PathVariable UUID id, 
            @RequestBody @Valid DeliveryOrderPatchRequest patchRequest) {
        OrderResponse response = orderService.patchOrder(id, patchRequest);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Cancelar pedido", description = "Requiere scope: order:update")
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('SCOPE_order:update')")
    public ResponseEntity<Void> cancelOrder(@PathVariable UUID id) {
        orderService.cancelOrder(id);
        return ResponseEntity.noContent().build();
    }

    // --- ENDPOINT SOLICITADO PARA COMUNICACIÓN ENTRE MICROSERVICIOS ---
    @Operation(summary = "Verificar existencia (Interno)", description = "Devuelve true si el pedido existe. Usado por Billing.")
    @PostMapping("/exists/{id}")
    @PreAuthorize("hasAuthority('SCOPE_order:view')") // Se necesita permiso de ver para verificar existencia
    public boolean orderExists(@PathVariable UUID id) {
        try {
             // Reutilizamos la lógica del servicio. 
             // Nota: Si tu servicio lanza excepción cuando no existe, 
             // el GlobalExceptionHandler devolverá 400/404, lo cual el FeignClient de Billing debe manejar.
            return orderService.orderExists(id);
        } catch (Exception e) {
            return false;
        }
    }
}