namespace FleetService.Services;

/// <summary>
/// Background service que inicia el listener de resultados de validación de drivers.
/// Se ejecuta una sola vez cuando la aplicación inicia.
/// </summary>
public class DriverValidationResultBackgroundService : BackgroundService
{
    private readonly IDriverValidationResultListener _listener;
    private readonly ILogger<DriverValidationResultBackgroundService> _logger;

    public DriverValidationResultBackgroundService(
        IDriverValidationResultListener listener,
        ILogger<DriverValidationResultBackgroundService> logger)
    {
        _listener = listener;
        _logger = logger;
    }

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        try
        {
            _logger.LogInformation("Iniciando DriverValidationResultBackgroundService...");
            await _listener.StartListeningAsync();
            
            // Mantener el listener activo mientras la aplicación esté corriendo
            while (!stoppingToken.IsCancellationRequested)
            {
                await Task.Delay(1000, stoppingToken);
            }
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error en DriverValidationResultBackgroundService");
        }
    }
}

/// <summary>
/// Background service que inicia el listener de compensación de drivers.
/// Se ejecuta una sola vez cuando la aplicación inicia.
/// </summary>
public class DriverCompensationBackgroundService : BackgroundService
{
    private readonly IDriverCompensationListener _listener;
    private readonly ILogger<DriverCompensationBackgroundService> _logger;

    public DriverCompensationBackgroundService(
        IDriverCompensationListener listener,
        ILogger<DriverCompensationBackgroundService> logger)
    {
        _listener = listener;
        _logger = logger;
    }

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        try
        {
            _logger.LogInformation("Iniciando DriverCompensationBackgroundService...");
            await _listener.StartListeningAsync();
            
            // Mantener el listener activo mientras la aplicación esté corriendo
            while (!stoppingToken.IsCancellationRequested)
            {
                await Task.Delay(1000, stoppingToken);
            }
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error en DriverCompensationBackgroundService");
        }
    }
}

/// <summary>
/// Background service que inicia el listener de creación de drivers desde auth-ms.
/// Cuando auth-ms registra un usuario DRIVER, este listener crea el driver en fleet DB.
/// </summary>
public class DriverUserCreatedBackgroundService : BackgroundService
{
    private readonly IDriverUserCreatedListener _listener;
    private readonly ILogger<DriverUserCreatedBackgroundService> _logger;

    public DriverUserCreatedBackgroundService(
        IDriverUserCreatedListener listener,
        ILogger<DriverUserCreatedBackgroundService> logger)
    {
        _listener = listener;
        _logger = logger;
    }

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        try
        {
            _logger.LogInformation("Iniciando DriverUserCreatedBackgroundService...");
            await _listener.StartListeningAsync();
            
            while (!stoppingToken.IsCancellationRequested)
            {
                await Task.Delay(1000, stoppingToken);
            }
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error en DriverUserCreatedBackgroundService");
        }
    }
}
