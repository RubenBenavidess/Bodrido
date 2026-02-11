package com.espe.edu.ec.customer_ms.dtos.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;



/**
 * Evento de compensación: cuando un usuario driver es eliminado o desactivado,
 * customer-ms publica esto para notificar a FleetService que lo marque como inactivo.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DriverCompensationEvent {
    private UUID driverId;
    private UUID userId;
    private String reason;
    private String action = "driver_compensation";
    private long timestamp = System.currentTimeMillis();
}
