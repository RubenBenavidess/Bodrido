namespace FleetService.Models;

/// <summary>
/// Estados de la saga de validación de conductores.
/// Similar a SagaStep de order-ms pero para drivers.
/// </summary>
public enum DriverValidationSagaStep
{
    // Esperando respuesta de validación del usuario en customer-ms
    WAITING_VERIFICATION,
    
    // Usuario validado exitosamente por customer-ms
    VERIFIED_SUCCESS,
    
    // Validación falló (usuario no existe o está inactivo)
    VERIFICATION_FAILED,
    
    // Timeout excedido durante validación
    VERIFICATION_TIMEOUT,
    
    // Compensación en progreso (rollback de customer-ms)
    COMPENSATING,
    
    // Compensación completada
    COMPENSATED
}
