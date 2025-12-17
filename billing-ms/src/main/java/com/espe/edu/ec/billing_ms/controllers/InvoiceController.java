package com.espe.edu.ec.billing_ms.controllers;

import com.espe.edu.ec.billing_ms.dtos.InvoiceRequest;
import com.espe.edu.ec.billing_ms.dtos.InvoiceResponse;
import com.espe.edu.ec.billing_ms.services.InvoiceService;
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
@RequestMapping("/invoices")
@RequiredArgsConstructor
@Tag(name = "Facturación", description = "Gestión de facturas electrónicas")
public class InvoiceController {

    private final InvoiceService invoiceService;

    @Operation(summary = "Crear borrador de factura", description = "Crea una nueva factura en estado DRAFT asociada a una orden.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Factura creada exitosamente", 
            content = @Content(schema = @Schema(implementation = InvoiceResponse.class))),
        @ApiResponse(responseCode = "400", description = "Datos inválidos o factura ya existente para la orden", content = @Content)
    })
    @PostMapping
    public ResponseEntity<InvoiceResponse> createInvoice(@RequestBody @Valid InvoiceRequest request) {
        InvoiceResponse response = invoiceService.createInvoice(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Emitir factura", description = "Cambia el estado de una factura de DRAFT a ISSUED y genera la fecha de emisión.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Factura emitida correctamente", 
            content = @Content(schema = @Schema(implementation = InvoiceResponse.class))),
        @ApiResponse(responseCode = "400", description = "La factura no está en estado DRAFT", content = @Content),
        @ApiResponse(responseCode = "404", description = "Factura no encontrada", content = @Content)
    })
    @PostMapping("/{id}/issue")
    public ResponseEntity<InvoiceResponse> issueInvoice(@PathVariable UUID id) {
        InvoiceResponse response = invoiceService.issueInvoice(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Buscar factura por Orden", description = "Obtiene la factura asociada a un ID de orden específico.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Factura encontrada", 
            content = @Content(schema = @Schema(implementation = InvoiceResponse.class))),
        @ApiResponse(responseCode = "404", description = "No existe factura para esa orden", content = @Content)
    })
    @GetMapping("/order/{orderId}")
    public ResponseEntity<InvoiceResponse> getInvoiceByOrderId(@PathVariable UUID orderId) {
        InvoiceResponse response = invoiceService.getInvoiceByOrderId(orderId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Listar todas las facturas", description = "Devuelve el historial completo de facturas.")
    @GetMapping
    public ResponseEntity<List<InvoiceResponse>> getAllInvoices() {
        List<InvoiceResponse> invoices = invoiceService.getAllInvoices();
        if (invoices.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(invoices);
    }
}