package com.espe.edu.ec.customer_ms.dtos.events;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;


/**
 * Respuesta que customer-ms envía a FleetService cuando valida un driver.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DriverValidationResultEvent {
    private UUID driverId;
    private UUID userId;
    private boolean isValid;
    private String errorMessage;
    private String action = "driver_validation_result";
    private long timestamp = System.currentTimeMillis();
}