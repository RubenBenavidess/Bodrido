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
/// Interfaz para escuchar respuestas de validación de drivers desde customer-ms.
/// </summary>
public interface IDriverValidationResultListener
{
    Task StartListeningAsync();
}

/// <summary>
/// Escucha eventos de validación de drivers desde customer-ms.
/// Actualiza el estado del driver según si la validación fue exitosa o no.
/// </summary>
public class DriverValidationResultListener : IDriverValidationResultListener
{
    private readonly IConnection _connection;
    private readonly IServiceProvider _serviceProvider;
    private readonly ILogger<DriverValidationResultListener> _logger;
    private IModel? _channel;

    public DriverValidationResultListener(
        IConnection connection,
        IServiceProvider serviceProvider,
        ILogger<DriverValidationResultListener> logger)
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
                exchange: RabbitMQDriverConfig.DRIVER_VALIDATION_RESULT_EXCHANGE,
                type: ExchangeType.Topic,
                durable: true,
                autoDelete: false
            );

            // Declarar queue
            _channel.QueueDeclare(
                queue: RabbitMQDriverConfig.DRIVER_VALIDATION_RESULT_QUEUE,
                durable: true,
                exclusive: false,
                autoDelete: false,
                arguments: null
            );

            // Bindear queue a exchange
            _channel.QueueBind(
                queue: RabbitMQDriverConfig.DRIVER_VALIDATION_RESULT_QUEUE,
                exchange: RabbitMQDriverConfig.DRIVER_VALIDATION_RESULT_EXCHANGE,
                routingKey: RabbitMQDriverConfig.DRIVER_VALIDATION_RESULT_ROUTING_KEY
            );

            var consumer = new AsyncEventingBasicConsumer(_channel);
            consumer.Received += HandleValidationResultAsync;

            _channel.BasicConsume(
                queue: RabbitMQDriverConfig.DRIVER_VALIDATION_RESULT_QUEUE,
                autoAck: false,
                consumer: consumer
            );

            _logger.LogInformation(
                "✓ Listener de resultados de validación de drivers iniciado en queue: {}",
                RabbitMQDriverConfig.DRIVER_VALIDATION_RESULT_QUEUE
            );

            await Task.CompletedTask;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "✗ Error iniciando listener de validación de drivers");
            throw;
        }
    }

    private async Task HandleValidationResultAsync(object model, BasicDeliverEventArgs ea)
    {
        try
        {
            var body = ea.Body.ToArray();
            var message = Encoding.UTF8.GetString(body);
            var result = JsonSerializer.Deserialize<DriverValidationResultEvent>(message);

            if (result == null)
            {
                _logger.LogWarning("No se pudo deserializar resultado de validación de driver");
                _channel?.BasicAck(ea.DeliveryTag, false);
                return;
            }

            _logger.LogInformation(
                "📨 Resultado de validación recibido: DriverId={}, IsValid={}, Error={}",
                result.DriverId, result.IsValid, result.ErrorMessage ?? "None"
            );

            using var scope = _serviceProvider.CreateScope();
            var context = scope.ServiceProvider.GetRequiredService<FleetContext>();

            var driver = await context.Drivers.FindAsync(result.DriverId);
            if (driver == null)
            {
                _logger.LogWarning("Driver no encontrado: {}", result.DriverId);
                _channel?.BasicAck(ea.DeliveryTag, false);
                return;
            }

            using var transaction = await context.Database.BeginTransactionAsync();
            try
            {
                if (result.IsValid)
                {
                    // Validación exitosa: cambiar a AVAILABLE
                    driver.ValidationSagaStep = DriverValidationSagaStep.VERIFIED_SUCCESS;
                    driver.Status = DriverStatus.AVAILABLE;
                    driver.IsValidationCompleted = true;
                    _logger.LogInformation("✓ Driver validado exitosamente: DriverId={}", result.DriverId);
                }
                else
                {
                    // Validación falló: cambiar a INACTIVE
                    driver.ValidationSagaStep = DriverValidationSagaStep.VERIFICATION_FAILED;
                    driver.Status = DriverStatus.INACTIVE;
                    driver.ValidationSagaReason = result.ErrorMessage ?? "Validation failed";
                    driver.IsValidationCompleted = true;
                    _logger.LogWarning(
                        "✗ Driver rechazado en validación: DriverId={}, Reason={}",
                        result.DriverId, result.ErrorMessage
                    );
                }

                driver.UpdatedAt = DateTime.UtcNow;
                await context.SaveChangesAsync();
                await transaction.CommitAsync();
            }
            catch (Exception ex)
            {
                await transaction.RollbackAsync();
                _logger.LogError(ex, "Error actualizando driver después de validación: DriverId={}", result.DriverId);
                throw;
            }

            _channel?.BasicAck(ea.DeliveryTag, false);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error procesando resultado de validación de driver");
            _channel?.BasicNack(ea.DeliveryTag, false, false);
        }
    }
}
