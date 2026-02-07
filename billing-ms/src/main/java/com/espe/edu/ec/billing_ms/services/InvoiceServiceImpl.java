package com.espe.edu.ec.billing_ms.services;

import com.espe.edu.ec.billing_ms.dtos.InvoiceRequest;
import com.espe.edu.ec.billing_ms.dtos.InvoiceResponse;
import com.espe.edu.ec.billing_ms.event_producers.BillingEventProducer;
import com.espe.edu.ec.billing_ms.mappers.InvoiceMapper;
import com.espe.edu.ec.billing_ms.models.Invoice;
import com.espe.edu.ec.billing_ms.model_enums.InvoiceStatus;
import com.espe.edu.ec.billing_ms.repositories.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final BillingEventProducer billingEventProducer;

    @Override
    @Transactional
    public InvoiceResponse createInvoice(InvoiceRequest request) {

        if (invoiceRepository.findByOrderId(request.getOrderId()).isPresent())
            throw new IllegalArgumentException("Ya existe una factura para la orden: " + request.getOrderId());

        log.info("Creando invoice para orden: {}", request.getOrderId());
        
        Invoice invoice = InvoiceMapper.requestToEntity(request);
        Invoice savedInvoice = invoiceRepository.save(invoice);
        
        // Publicar evento de creación de invoice
        try {
            billingEventProducer.publishInvoiceCreatedEvent(savedInvoice);
        } catch (Exception e) {
            log.error("Error publicando evento de creación de invoice para orderId={}", request.getOrderId(), e);
            throw new RuntimeException("Error al publicar evento de invoice", e);
        }
        
        return InvoiceMapper.entityToResponse(savedInvoice);

    }

    @Override
    @Transactional
    public InvoiceResponse issueInvoice(UUID id) {

        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Factura no encontrada con ID: " + id));

        if (invoice.getStatus() != InvoiceStatus.DRAFT)
            throw new IllegalStateException("Solo se pueden emitir facturas en estado DRAFT. Estado actual: " + invoice.getStatus());
        
        invoice.setStatus(InvoiceStatus.ISSUED);
        invoice.setIssuedAt(LocalDateTime.now());
        
        // Complete XML data
        // invoice.setXmlData(generateXml(...));

        Invoice updatedInvoice = invoiceRepository.save(invoice);
        
        // Publicar evento de emisión de invoice
        try {
            billingEventProducer.publishInvoiceIssuedEvent(updatedInvoice);
        } catch (Exception e) {
            log.error("Error publicando evento de emisión de invoice para invoiceId={}", id, e);
            throw new RuntimeException("Error al publicar evento de invoice", e);
        }
        
        return InvoiceMapper.entityToResponse(updatedInvoice);

    }

    @Override
    public InvoiceResponse getInvoiceByOrderId(UUID orderId) {

        Invoice invoice = invoiceRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró factura para la orden: " + orderId));
        return InvoiceMapper.entityToResponse(invoice);

    }

    @Override
    public List<InvoiceResponse> getAllInvoices() {
        
        return invoiceRepository.findAll().stream()
                .map(InvoiceMapper::entityToResponse)
                .toList();

    }
}