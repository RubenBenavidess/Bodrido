package com.espe.edu.ec.order_ms.model_enums;

public enum OrderStatus {
    PENDING, // Orden creada, esperando verificación de customer
    CREATED, // Orden verificada, lista para operaciones
    ASSIGNMENT_PENDING, // Esperando validación de asignación en FleetService
    ASSIGNED, // Conductor y vehículo asignados y validados
    PICKUP_PENDING, // Esperando validación de recogida en FleetService
    PICKED_UP, // Paquete recogido por el conductor
    IN_ROUTE, // En ruta hacia el destino
    DELIVERED, // Entregado al cliente
    CANCELLATION_PENDING, // Esperando confirmación de liberación de recursos en FleetService
    CANCELLED // Cancelado (recursos liberados si aplica)
}
