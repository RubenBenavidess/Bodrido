package com.espe.edu.ec.customer_ms.dtos.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Evento publicado por FleetService cuando crea un conductor.
 * Solicita a customer-ms que valide que el UserId existe y tiene rol DRIVER.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DriverValidationEvent {
    private UUID driverId;
    private UUID userId;
    private String licenseNumber;
    private String action = "driver_validation";
    private long timestamp = System.currentTimeMillis();
}
