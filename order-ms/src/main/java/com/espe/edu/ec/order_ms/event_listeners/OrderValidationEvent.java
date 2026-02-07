package com.espe.edu.ec.order_ms.event_listeners;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Evento de validación que viene desde otros microservicios (FleetService, etc)
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class OrderValidationEvent {

    private UUID orderId;
    private String validationType; // "resources_assigned", etc
    private boolean success;
    private String errorMessage;
    private String sourceService; // "fleet-ms", "billing-ms", etc

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
}
