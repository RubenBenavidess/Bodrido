package com.espe.edu.ec.order_ms.model_enums;

public enum OrderStatus {
    PENDING,                // Orden creada, esperando verificación de customer
    CREATED,                // Orden verificada, lista para operaciones
    ASSIGNMENT_PENDING,
    ASSIGNED,
    PICKED_UP, 
    IN_ROUTE, 
    DELIVERED,
    CANCELLED
}
