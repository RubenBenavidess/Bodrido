package com.espe.edu.ec.billing_ms.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceRequest {

    @NotNull(message = "El ID de la orden es requerido")
    private UUID orderId;

    @NotNull(message = "El RUC/Cédula es requerido")
    @Size(max = 20, message = "El documento no puede exceder 20 caracteres")
    private String customerTaxId;

    @NotNull
    @PositiveOrZero
    private BigDecimal subtotal;

    @NotNull
    @PositiveOrZero
    private BigDecimal taxAmount;

    @NotNull
    @PositiveOrZero
    private BigDecimal total; 

}