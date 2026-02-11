namespace FleetService.Models.Events;

/// <summary>
/// DTO para recibir el evento de validación de asignación desde order-ms.
/// Mapea los campos que order-ms envía via Jackson (camelCase por defecto).
/// </summary>
public class IncomingOrderValidationRequest
{
    public Guid OrderId { get; set; }
    public Guid DriverId { get; set; }
    public string VehicleId { get; set; } = string.Empty;
    public string ValidationType { get; set; } = string.Empty;
    public string SourceService { get; set; } = string.Empty;
    public string? Timestamp { get; set; }
}
