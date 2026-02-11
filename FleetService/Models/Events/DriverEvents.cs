namespace FleetService.Models.Events;

/// <summary>
/// Evento publicado por FleetService cuando se crea un conductor.
/// Se envía a customer-ms para validar que el usuario existe.
/// </summary>
public class DriverValidationEvent
{
    public Guid DriverId { get; set; }
    public Guid UserId { get; set; }
    public string LicenseNumber { get; set; }
    public string Action { get; set; } = "driver_created";
    public DateTime Timestamp { get; set; } = DateTime.UtcNow;
}

/// <summary>
/// Respuesta de customer-ms sobre la validación del conductor.
/// FleetService la recibe para confirmar o rechazar.
/// </summary>
public class DriverValidationResultEvent
{
    public Guid DriverId { get; set; }
    public Guid UserId { get; set; }
    public bool IsValid { get; set; }
    public string? ErrorMessage { get; set; }
    public string Action { get; set; } = "driver_validation_result";
    public DateTime Timestamp { get; set; } = DateTime.UtcNow;
}

/// <summary>
/// Evento de compensación desde customer-ms cuando un driver debe ser cancelado.
/// Ej: si el usuario es eliminado de auth-ms o se revoca su acceso.
/// </summary>
public class DriverCompensationEvent
{
    public Guid DriverId { get; set; }
    public Guid UserId { get; set; }
    public string Reason { get; set; }
    public string Action { get; set; } = "driver_compensation";
    public DateTime Timestamp { get; set; } = DateTime.UtcNow;
}
