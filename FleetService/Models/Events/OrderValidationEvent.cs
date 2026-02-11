namespace FleetService.Models.Events;

public class OrderValidationEvent
{
    public Guid OrderId { get; set; }
    public string ValidationType { get; set; } // "resources_assigned", etc
    public bool Success { get; set; }
    public string? ErrorMessage { get; set; }
    public string SourceService { get; set; } = "fleet-ms";
    public string Timestamp { get; set; } = DateTime.UtcNow.ToString("yyyy-MM-ddTHH:mm:ss");
}
