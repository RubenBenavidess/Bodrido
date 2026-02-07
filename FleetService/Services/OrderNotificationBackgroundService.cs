namespace FleetService.Services;

/// <summary>
/// Servicio background que inicia el listener de eventos de notificación
/// cuando la aplicación se inicia.
/// </summary>
public class OrderNotificationBackgroundService : BackgroundService
{
    private readonly IOrderNotificationListener _listener;
    private readonly ILogger<OrderNotificationBackgroundService> _logger;

    public OrderNotificationBackgroundService(
        IOrderNotificationListener listener,
        ILogger<OrderNotificationBackgroundService> logger)
    {
        _listener = listener;
        _logger = logger;
    }

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        try
        {
            _logger.LogInformation("Iniciando servicio background de escucha de notificaciones");
            await _listener.StartListeningAsync();
            
            // Mantener el servicio activo
            while (!stoppingToken.IsCancellationRequested)
            {
                await Task.Delay(1000, stoppingToken);
            }
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error en servicio background de escucha de notificaciones");
            throw;
        }
    }
}
