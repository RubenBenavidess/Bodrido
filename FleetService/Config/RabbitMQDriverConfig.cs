using RabbitMQ.Client;
using RabbitMQ.Client.Events;

namespace FleetService.Config;

/// <summary>
/// Configuración de RabbitMQ para FleetService.
/// Define los exchanges, queues y bindings para:
/// - Enviar validaciones de drivers a customer-ms
/// - Recibir respuestas de validación
/// - Recibir eventos de compensación (rollback)
/// </summary>
public static class RabbitMQDriverConfig
{
    // ============== VALIDACIÓN DE DRIVERS - FleetService → customer-ms ==============
    public const string DRIVER_VALIDATION_EXCHANGE = "drivers-validation.exchange";
    public const string DRIVER_VALIDATION_ROUTING_KEY = "drivers.validation.routing";
    
    // ============== RESPUESTA DE VALIDACIÓN - customer-ms → FleetService ==============
    public const string DRIVER_VALIDATION_RESULT_EXCHANGE = "driver-validation-result.exchange";
    public const string DRIVER_VALIDATION_RESULT_QUEUE = "driver.validation.result";
    public const string DRIVER_VALIDATION_RESULT_ROUTING_KEY = "driver.validation.result";
    
    // ============== COMPENSACIÓN DE DRIVERS - customer-ms → FleetService ==============
    public const string DRIVER_COMPENSATION_EXCHANGE = "driver-compensation.exchange";
    public const string DRIVER_COMPENSATION_QUEUE = "driver.compensation";
    public const string DRIVER_COMPENSATION_ROUTING_KEY = "driver.compensation.routing";
}

/// <summary>
/// Servicio que establece la conexión y crea los canales de RabbitMQ.
/// Centraliza la lógica de conexión para reutilización.
/// </summary>
public class RabbitMQConnectionService
{
    private readonly IConnection _connection;
    private IModel? _channel;
    private readonly ILogger<RabbitMQConnectionService> _logger;

    public RabbitMQConnectionService(IConnection connection, ILogger<RabbitMQConnectionService> logger)
    {
        _connection = connection;
        _logger = logger;
    }

    public IModel GetChannel()
    {
        if (_channel == null)
        {
            _channel = _connection.CreateModel();
            _logger.LogInformation("Canal de RabbitMQ creado");
        }
        return _channel;
    }
}
