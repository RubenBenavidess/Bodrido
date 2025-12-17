package com.espe.edu.ec.billing_ms.services;

import com.espe.edu.ec.billing_ms.dtos.InvoiceRequest;
import com.espe.edu.ec.billing_ms.dtos.InvoiceResponse;

import java.util.List;
import java.util.UUID;

public interface InvoiceService {
    InvoiceResponse createInvoice(InvoiceRequest request);
    InvoiceResponse issueInvoice(UUID id); // Pasa de DRAFT a ISSUED
    InvoiceResponse getInvoiceByOrderId(UUID orderId);
    List<InvoiceResponse> getAllInvoices();
}