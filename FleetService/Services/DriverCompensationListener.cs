using System.Text;
using System.Text.Json;
using FleetService.Config;
using FleetService.Data;
using FleetService.Models;
using FleetService.Models.Events;
using RabbitMQ.Client;
using RabbitMQ.Client.Events;

namespace FleetService.Services;

/// <summary>
/// Interfaz para escuchar eventos de compensación de drivers desde customer-ms.
/// </summary>
public interface IDriverCompensationListener
{
    Task StartListeningAsync();
}

/// <summary>
/// Escucha eventos de compensación cuando un driver debe ser cancelado.
/// Ej: si el usuario es eliminado de auth-ms o se revoca su acceso.
/// </summary>
public class DriverCompensationListener : IDriverCompensationListener
{
    private readonly IConnection _connection;
    private readonly IServiceProvider _serviceProvider;
    private readonly ILogger<DriverCompensationListener> _logger;
    private IModel? _channel;

    public DriverCompensationListener(
        IConnection connection,
        IServiceProvider serviceProvider,
        ILogger<DriverCompensationListener> logger)
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

            // Declarar exchange
            _channel.ExchangeDeclare(
                exchange: RabbitMQDriverConfig.DRIVER_COMPENSATION_EXCHANGE,
                type: ExchangeType.Topic,
                durable: true,
                autoDelete: false
            );

            // Declarar queue
            _channel.QueueDeclare(
                queue: RabbitMQDriverConfig.DRIVER_COMPENSATION_QUEUE,
                durable: true,
                exclusive: false,
                autoDelete: false,
                arguments: null
            );

            // Bindear queue a exchange
            _channel.QueueBind(
                queue: RabbitMQDriverConfig.DRIVER_COMPENSATION_QUEUE,
                exchange: RabbitMQDriverConfig.DRIVER_COMPENSATION_EXCHANGE,
                routingKey: RabbitMQDriverConfig.DRIVER_COMPENSATION_ROUTING_KEY
            );

            var consumer = new AsyncEventingBasicConsumer(_channel);
            consumer.Received += HandleCompensationAsync;

            _channel.BasicConsume(
                queue: RabbitMQDriverConfig.DRIVER_COMPENSATION_QUEUE,
                autoAck: false,
                consumer: consumer
            );

            _logger.LogInformation(
                "✓ Listener de compensación de drivers iniciado en queue: {}",
                RabbitMQDriverConfig.DRIVER_COMPENSATION_QUEUE
            );

            await Task.CompletedTask;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "✗ Error iniciando listener de compensación de drivers");
            throw;
        }
    }

    private async Task HandleCompensationAsync(object model, BasicDeliverEventArgs ea)
    {
        try
        {
            var body = ea.Body.ToArray();
            var message = Encoding.UTF8.GetString(body);
            var compensation = JsonSerializer.Deserialize<DriverCompensationEvent>(message);

            if (compensation == null)
            {
                _logger.LogWarning("No se pudo deserializar evento de compensación de driver");
                _channel?.BasicAck(ea.DeliveryTag, false);
                return;
            }

            _logger.LogWarning(
                "🔄 Evento de compensación recibido: DriverId={}, Reason={}",
                compensation.DriverId, compensation.Reason
            );

            using var scope = _serviceProvider.CreateScope();
            var context = scope.ServiceProvider.GetRequiredService<FleetContext>();

            var driver = await context.Drivers.FindAsync(compensation.DriverId);
            if (driver == null)
            {
                _logger.LogWarning("Driver no encontrado para compensación: {}", compensation.DriverId);
                _channel?.BasicAck(ea.DeliveryTag, false);
                return;
            }

            using var transaction = await context.Database.BeginTransactionAsync();
            try
            {
                // Marcar driver como compensado e inactivo
                driver.ValidationSagaStep = DriverValidationSagaStep.COMPENSATED;
                driver.Status = DriverStatus.INACTIVE;
                driver.ValidationSagaReason = $"Compensado: {compensation.Reason}";
                driver.IsValidationCompleted = true;
                driver.UpdatedAt = DateTime.UtcNow;

                // Si el driver estaba asignado a un vehículo, liberarlo
                if (driver.CurrentVehicleId.HasValue)
                {
                    var vehicle = await context.Vehicles.FindAsync(driver.CurrentVehicleId);
                    if (vehicle != null)
                    {
                        vehicle.IsAssigned = false;
                        vehicle.UpdatedAt = DateTime.UtcNow;
                        _logger.LogInformation("Vehículo liberado durante compensación: VehicleId={}", vehicle.Id);
                    }
                }

                await context.SaveChangesAsync();
                await transaction.CommitAsync();

                _logger.LogInformation("✓ Compensación completada para driver: DriverId={}", compensation.DriverId);
            }
            catch (Exception ex)
            {
                await transaction.RollbackAsync();
                _logger.LogError(ex, "Error procesando compensación de driver: DriverId={}", compensation.DriverId);
                throw;
            }

            _channel?.BasicAck(ea.DeliveryTag, false);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error procesando evento de compensación de driver");
            _channel?.BasicNack(ea.DeliveryTag, false, false);
        }
    }
}
