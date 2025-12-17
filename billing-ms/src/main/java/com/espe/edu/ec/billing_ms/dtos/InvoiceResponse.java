package com.espe.edu.ec.billing_ms.dtos;

import com.espe.edu.ec.billing_ms.model_enums.InvoiceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceResponse {

    private UUID id;
    private UUID orderId;
    private String customerTaxId;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal total;
    private InvoiceStatus status;
    private LocalDateTime issuedAt;
    private String xmlData; 

}