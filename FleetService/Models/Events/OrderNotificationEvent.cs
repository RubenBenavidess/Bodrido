namespace FleetService.Models.Events;

public class OrderNotificationEvent
{
    public Guid Id { get; set; }
    public string Microservice { get; set; } = "order-ms";
    public string Action { get; set; }
    public Guid OrderId { get; set; }
    public string EntityType { get; set; } = "ORDER";
    public string Message { get; set; }
    public DateTime Timestamp { get; set; }
    public Dictionary<string, object> Data { get; set; } = new();
    public string Severity { get; set; }
    
    public string GetTimeStamp()
    {
        return Timestamp.ToString("yyyy-MM-ddTHH:mm:ss");
    }
}
