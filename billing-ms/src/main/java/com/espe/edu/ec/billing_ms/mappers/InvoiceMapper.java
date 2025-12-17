package com.espe.edu.ec.billing_ms.mappers;

import com.espe.edu.ec.billing_ms.dtos.InvoiceRequest;
import com.espe.edu.ec.billing_ms.dtos.InvoiceResponse;
import com.espe.edu.ec.billing_ms.models.Invoice;
import com.espe.edu.ec.billing_ms.model_enums.InvoiceStatus;

public final class InvoiceMapper {

    private InvoiceMapper() {
        throw new UnsupportedOperationException("Clase utilitaria");
    }

    public static Invoice requestToEntity(InvoiceRequest request) {

        if (request == null) throw new IllegalArgumentException("Datos de factura inválidos.");          

        return Invoice.builder()
                .orderId(request.getOrderId())
                .customerTaxId(request.getCustomerTaxId())
                .subtotal(request.getSubtotal())
                .taxAmount(request.getTaxAmount())
                .total(request.getTotal()) 
                .status(InvoiceStatus.DRAFT) 
                .build();
    }

    public static InvoiceResponse entityToResponse(Invoice invoice) {
        if (invoice == null) return null;

        return InvoiceResponse.builder()
                .id(invoice.getId())
                .orderId(invoice.getOrderId())
                .customerTaxId(invoice.getCustomerTaxId())
                .subtotal(invoice.getSubtotal())
                .taxAmount(invoice.getTaxAmount())
                .total(invoice.getTotal())
                .status(invoice.getStatus())
                .issuedAt(invoice.getIssuedAt())
                .xmlData(invoice.getXmlData())
                .build();
    }
}