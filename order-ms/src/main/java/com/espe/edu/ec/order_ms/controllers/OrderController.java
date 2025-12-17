package com.espe.edu.ec.order_ms.controllers;

import com.espe.edu.ec.order_ms.dtos.DeliveryOrderPatchRequest;
import com.espe.edu.ec.order_ms.dtos.OrderRequest;
import com.espe.edu.ec.order_ms.dtos.OrderResponse;
import com.espe.edu.ec.order_ms.services.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Pedidos", description = "Operaciones relacionadas con la creación, seguimiento y cancelación de pedidos")
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "Crear un nuevo pedido", description = "Registra un pedido, calcula tarifas y asigna estado inicial.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Pedido creado exitosamente", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderResponse.class))),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos o fuera de cobertura", content = @Content),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = @Content)
    })
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody @Valid OrderRequest orderRequest) {
        OrderResponse response = orderService.createOrder(orderRequest);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Obtener pedido por ID", description = "Retorna los detalles de un pedido específico.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Pedido encontrado", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderResponse.class))),
        @ApiResponse(responseCode = "404", description = "Pedido no encontrado", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable UUID id) {
        OrderResponse response = orderService.getOrder(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Listar todos los pedidos", description = "Devuelve una lista completa de todos los pedidos registrados.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de pedidos recuperada", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderResponse.class))),
        @ApiResponse(responseCode = "204", description = "No hay pedidos registrados", content = @Content)
    })
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        List<OrderResponse> orders = orderService.getOrders();
        
        if (orders.isEmpty()) return ResponseEntity.noContent().build(); 
        
        return ResponseEntity.ok(orders); 
    }

    @Operation(summary = "Actualizar pedido (Patch)", description = "Permite modificar instrucciones o coordenadas de entrega.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Pedido actualizado correctamente", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderResponse.class))),
        @ApiResponse(responseCode = "400", description = "Error de validación o estado incorrecto", content = @Content),
        @ApiResponse(responseCode = "404", description = "Pedido no encontrado", content = @Content)
    })
    @PatchMapping("/{id}")
    public ResponseEntity<OrderResponse> patchOrder(
            @PathVariable UUID id, 
            @RequestBody DeliveryOrderPatchRequest patchRequest) {
        OrderResponse response = orderService.patchOrder(id, patchRequest);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Cancelar pedido", description = "Cancela un pedido si se encuentra en estado CREATED.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Pedido cancelado exitosamente"),
        @ApiResponse(responseCode = "400", description = "No se puede cancelar en el estado actual", content = @Content),
        @ApiResponse(responseCode = "404", description = "Pedido no encontrado", content = @Content)
    })
    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelOrder(@PathVariable UUID id) {
        orderService.cancelOrder(id);
        return ResponseEntity.noContent().build();
    }
}