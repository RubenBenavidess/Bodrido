using System.Text;
using System.Text.Json;
using FleetService.Config;
using FleetService.Data;
using FleetService.Models;
using Microsoft.EntityFrameworkCore;
using FleetService.Models.Events;
using RabbitMQ.Client;
using RabbitMQ.Client.Events;

namespace FleetService.Services;

/// <summary>
/// DTO que representa el evento publicado por auth-ms cuando se crea un usuario DRIVER.
/// </summary>
public class DriverUserCreatedEvent
{
    public string? user_id { get; set; }
    public string? email { get; set; }
    public string? username { get; set; }
    public int? role_id { get; set; }
    public string? vehicle_type { get; set; }
    public int? zone_id { get; set; }
    public string? action { get; set; }
    public string? message { get; set; }
    public long? timestamp { get; set; }
}

/// <summary>
/// Interfaz para el listener de eventos de creación de drivers desde auth-ms.
/// </summary>
public interface IDriverUserCreatedListener
{
    Task StartListeningAsync();
}

/// <summary>
/// Escucha eventos de creación de usuarios DRIVER desde auth-ms.
/// Cuando auth-ms registra un usuario con rol DRIVER, este listener:
/// 1. Recibe el evento
/// 2. Crea el Driver en fleet DB
/// 3. Publica un resultado (éxito/fallo) de vuelta a auth-ms
///    - Si falla → auth-ms desactiva el usuario (compensación)
/// </summary>
public class DriverUserCreatedListener : IDriverUserCreatedListener
{
    private readonly IConnection _connection;
    private readonly IServiceProvider _serviceProvider;
    private readonly ILogger<DriverUserCreatedListener> _logger;
    private IModel? _channel;

    // Exchange y queue para recibir eventos de auth-ms
    // IMPORTANTE: Usar queue PROPIA de FleetService para no competir con customer-ms
    // customer-ms usa "driver.user.created", FleetService usa "fleet.driver.user.created"
    // Ambas bindeadas al mismo exchange, así ambos reciben una copia del mensaje
    private const string EXCHANGE = "driver-user-created.exchange";
    private const string QUEUE = "fleet.driver.user.created";
    private const string ROUTING_KEY = "driver.user.created.routing";

    // Exchange para publicar resultado/rollback a auth-ms
    private const string ROLLBACK_EXCHANGE = "user-rollback.exchange";
    private const string ROLLBACK_ROUTING_KEY = "user.rollback.routing";

    public DriverUserCreatedListener(
        IConnection connection,
        IServiceProvider serviceProvider,
        ILogger<DriverUserCreatedListener> logger)
    {
        _connection = connection;
        _serviceProvider = serviceProvider;
        _logger = logger;
    }

    public async Task StartListeningAsync()
    {
        try
        {
            _channel = _connection.CreateModel();

            // Declarar exchange (debe coincidir con auth-ms)
            _channel.ExchangeDeclare(
                exchange: EXCHANGE,
                type: ExchangeType.Topic,
                durable: true,
                autoDelete: false
            );

            // Declarar queue propia de FleetService (no necesita DLX/TTL)
            _channel.QueueDeclare(
                queue: QUEUE,
                durable: true,
                exclusive: false,
                autoDelete: false,
                arguments: null
            );

            // Bindear queue a exchange
            _channel.QueueBind(
                queue: QUEUE,
                exchange: EXCHANGE,
                routingKey: ROUTING_KEY
            );

            // Declarar exchange de rollback para publicar resultados a auth-ms
            _channel.ExchangeDeclare(
                exchange: ROLLBACK_EXCHANGE,
                type: ExchangeType.Topic,
                durable: true,
                autoDelete: false
            );

            var consumer = new AsyncEventingBasicConsumer(_channel);
            consumer.Received += HandleDriverUserCreatedAsync;

            _channel.BasicConsume(
                queue: QUEUE,
                autoAck: false,
                consumer: consumer
            );

            _logger.LogInformation(
                "✓ Listener de creación de drivers desde auth-ms iniciado en queue: {}",
                QUEUE
            );

            await Task.CompletedTask;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "✗ Error iniciando listener de creación de drivers desde auth-ms");
            throw;
        }
    }

    private async Task HandleDriverUserCreatedAsync(object model, BasicDeliverEventArgs ea)
    {
        try
        {
            var body = ea.Body.ToArray();
            var messageStr = Encoding.UTF8.GetString(body);

            _logger.LogInformation("📨 [DRIVER-USER-CREATED] Evento recibido de auth-ms: {}", messageStr);

            var evt = JsonSerializer.Deserialize<DriverUserCreatedEvent>(messageStr);

            if (evt == null || string.IsNullOrEmpty(evt.user_id))
            {
                _logger.LogWarning("⚠ Evento inválido recibido (null o sin user_id)");
                _channel?.BasicAck(ea.DeliveryTag, false);
                return;
            }

            // Validar que el user_id sea un UUID válido
            if (!Guid.TryParse(evt.user_id, out var userId))
            {
                _logger.LogWarning("⚠ user_id inválido (no es UUID): {}", evt.user_id);
                PublishRollback(evt.user_id, evt.email, "user_id no es un UUID válido");
                _channel?.BasicAck(ea.DeliveryTag, false);
                return;
            }

            _logger.LogInformation(
                "► [DRIVER-USER-CREATED] Procesando: UserId={}, Email={}, VehicleType={}",
                userId, evt.email, evt.vehicle_type
            );

            using var scope = _serviceProvider.CreateScope();
            var context = scope.ServiceProvider.GetRequiredService<FleetContext>();

            // Verificar si ya existe un driver con este UserId
            var existingDriver = await context.Drivers
                .FirstOrDefaultAsync(d => d.UserId == userId);

            if (existingDriver != null)
            {
                _logger.LogWarning(
                    "⚠ Driver ya existe para UserId={}, DriverId={}. Ignorando evento.",
                    userId, existingDriver.Id
                );
                _channel?.BasicAck(ea.DeliveryTag, false);
                return;
            }

            using var transaction = await context.Database.BeginTransactionAsync();
            try
            {
                // Crear el driver en fleet DB
                var driver = new Driver
                {
                    Id = Guid.NewGuid(),
                    UserId = userId,
                    LicenseNumber = "PENDING", // Se actualizará después via API
                    LicenseCategory = LicenseCategory.B, // Default, se actualizará después
                    Status = DriverStatus.AVAILABLE,
                    ValidationSagaStep = DriverValidationSagaStep.VERIFIED_SUCCESS,
                    IsValidationCompleted = true,
                    CreatedAt = DateTime.UtcNow,
                    UpdatedAt = DateTime.UtcNow
                };

                context.Drivers.Add(driver);
                await context.SaveChangesAsync();
                await transaction.CommitAsync();

                _logger.LogInformation(
                    "✓ [DRIVER-USER-CREATED] Driver creado exitosamente: DriverId={}, UserId={}",
                    driver.Id, userId
                );
            }
            catch (Exception ex)
            {
                await transaction.RollbackAsync();
                _logger.LogError(ex,
                    "✗ [DRIVER-USER-CREATED] Error creando driver para UserId={}. Publicando rollback a auth-ms.",
                    userId
                );

                // Publicar rollback a auth-ms para desactivar el usuario
                PublishRollback(evt.user_id, evt.email, $"Error creando driver en FleetService: {ex.Message}");
            }

            _channel?.BasicAck(ea.DeliveryTag, false);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "✗ Error procesando evento de creación de driver de auth-ms");
            _channel?.BasicNack(ea.DeliveryTag, false, false);
        }
    }

    /// <summary>
    /// Publica un evento de rollback a auth-ms para desactivar el usuario
    /// cuando FleetService no puede crear el driver.
    /// </summary>
    private void PublishRollback(string userId, string? email, string reason)
    {
        try
        {
            using var channel = _connection.CreateModel();

            var rollbackEvent = new
            {
                user_id = userId,
                email = email ?? "unknown",
                reason = reason,
                source = "fleet-ms",
                action = "user_rollback",
                timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds()
            };

            var message = JsonSerializer.Serialize(rollbackEvent);
            var body = Encoding.UTF8.GetBytes(message);

            var properties = channel.CreateBasicProperties();
            properties.Persistent = true;
            properties.ContentType = "application/json";

            channel.BasicPublish(
                exchange: ROLLBACK_EXCHANGE,
                routingKey: ROLLBACK_ROUTING_KEY,
                basicProperties: properties,
                body: body
            );

            _logger.LogWarning(
                "🔄 [ROLLBACK] Evento de rollback publicado a auth-ms: UserId={}, Reason={}",
                userId, reason
            );
        }
        catch (Exception ex)
        {
            _logger.LogError(ex,
                "✗ Error publicando evento de rollback para UserId={}", userId
            );
        }
    }
}
