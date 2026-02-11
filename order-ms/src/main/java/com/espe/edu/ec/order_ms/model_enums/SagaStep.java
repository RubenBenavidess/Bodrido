package com.espe.edu.ec.order_ms.model_enums;

public enum SagaStep {
    // === Saga de verificación de customer ===
    WAITING_VERIFICATION,
    VERIFIED,
    REJECTED,
    TIMEOUT,

    // === Saga de asignación de conductor/vehículo ===
    WAITING_ASSIGNMENT_VERIFICATION,
    ASSIGNMENT_VERIFIED,

    // === Saga de cancelación (liberación de recursos en FleetService) ===
    WAITING_CANCELLATION_VERIFICATION,
    CANCELLATION_VERIFIED,
    CANCELLATION_TIMEOUT,

    // === Saga de pickup (validación de recursos en FleetService) ===
    WAITING_PICKUP_VERIFICATION,
    PICKUP_VERIFIED,
    PICKUP_REJECTED,
    PICKUP_TIMEOUT
}