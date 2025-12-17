package com.espe.edu.ec.billing_ms.repositories;

import com.espe.edu.ec.billing_ms.models.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    
    Optional<Invoice> findByOrderId(UUID orderId);
    
}