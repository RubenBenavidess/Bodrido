package com.espe.edu.ec.order_ms.controllers;

import com.espe.edu.ec.order_ms.dtos.AssignDriverRequest;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Tag(name = "Pedidos", description = "Operaciones de pedidos protegidas con JWT")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "Crear pedido", description = "Requiere scope: order:create")
    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_order:create')")
    public ResponseEntity<OrderResponse> createOrder(@RequestBody @Valid OrderRequest orderRequest) {
        OrderResponse response = orderService.createOrder(orderRequest);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // 1. OBTENER TODOS LOS PEDIDOS -> Solo ADMIN (order:view)
    @Operation(summary = "Listar todos los pedidos", description = "Exclusivo para ADMIN. Requiere scope: order:view")
    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_order:view')")
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        List<OrderResponse> orders = orderService.getOrders();
        if (orders.isEmpty()) return ResponseEntity.noContent().build(); 
        return ResponseEntity.ok(orders); 
    }

    // 2. OBTENER PEDIDO POR ID -> Solo ADMIN (order:view)
    @Operation(summary = "Obtener pedido por ID", description = "Exclusivo para ADMIN. Requiere scope: order:view")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_order:view')")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable UUID id) {
        OrderResponse response = orderService.getOrder(id);
        return ResponseEntity.ok(response);
    }

    // 3. y 4. OBTENER PEDIDOS POR CLIENTE -> Cliente (order:view_own) + Validación de Token
    @Operation(summary = "Listar pedidos de un cliente", description = "Exclusivo para el Cliente dueño de los datos.")
    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAuthority('SCOPE_order:view_own')")
    public ResponseEntity<List<OrderResponse>> getOrdersByCustomer(
            @PathVariable UUID customerId,
            @AuthenticationPrincipal Jwt jwt) { // Inyectamos el Token decodificado

        // VALIDACIÓN DE SEGURIDAD:
        // Extraemos el 'user_id' del payload del token y lo comparamos con el path variable
        String tokenUserId = jwt.getClaimAsString("user_id");
        
        if (tokenUserId == null || !tokenUserId.equals(customerId.toString())) {
            throw new AccessDeniedException("No tienes permiso para ver los pedidos de otro cliente.");
        }

        List<OrderResponse> orders = orderService.getOrdersByCustomer(customerId);
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

    @Operation(summary = "Verificar existencia (Interno)", description = "Usado por microservicio de Billing.")
    @PostMapping("/exists/{id}")
    @PreAuthorize("hasAuthority('SCOPE_order:view')") 
    public boolean orderExists(@PathVariable UUID id) {
        try {
            return orderService.orderExists(id);
        } catch (Exception e) {
            return false;
        }
    }

    @Operation(summary = "Asignar conductor y vehículo", description = "Permite a Administradores y Supervisores asignar los recursos de transporte a una orden.")
    @PatchMapping("/{id}/assign")
    @PreAuthorize("hasAuthority('SCOPE_order:update')")
    public ResponseEntity<OrderResponse> assignDriverAndVehicle(
            @PathVariable UUID id, 
            @RequestBody @Valid AssignDriverRequest request) {
        
        OrderResponse response = orderService.assignDriverAndVehicle(id, request);
        return ResponseEntity.ok(response);
    }
}