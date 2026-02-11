using System.Text;
using System.Text.Json;
using FleetService.Config;
using FleetService.Models.Events;
using RabbitMQ.Client;

namespace FleetService.Services;

/// <summary>
/// Interfaz para publicar eventos de validación de drivers en RabbitMQ.
/// </summary>
public interface IDriverValidationProducer
{
    Task PublishDriverValidationAsync(DriverValidationEvent evt);
}

/// <summary>
/// Implementación que publica eventos de validación de drivers a customer-ms.
/// Cuando se crea un conductor en FleetService, se envía un evento para 
/// verificar que el usuario (UserId) existe en auth-ms.
/// </summary>
public class DriverValidationProducer : IDriverValidationProducer
{
    private readonly IConnection _connection;
    private readonly ILogger<DriverValidationProducer> _logger;

    public DriverValidationProducer(IConnection connection, ILogger<DriverValidationProducer> logger)
    {
        _connection = connection;
        _logger = logger;
    }

    public async Task PublishDriverValidationAsync(DriverValidationEvent evt)
    {
        try
        {
            using (var channel = _connection.CreateModel())
            {
                // Asegurar que el exchange existe
                channel.ExchangeDeclare(
                    exchange: RabbitMQDriverConfig.DRIVER_VALIDATION_EXCHANGE,
                    type: ExchangeType.Topic,
                    durable: true,
                    autoDelete: false
                );

                var message = JsonSerializer.Serialize(evt);
                var messageBytes = Encoding.UTF8.GetBytes(message);

                var properties = channel.CreateBasicProperties();
                properties.Persistent = true;
                properties.ContentType = "application/json";

                channel.BasicPublish(
                    exchange: RabbitMQDriverConfig.DRIVER_VALIDATION_EXCHANGE,
                    routingKey: RabbitMQDriverConfig.DRIVER_VALIDATION_ROUTING_KEY,
                    basicProperties: properties,
                    body: messageBytes
                );

                _logger.LogInformation(
                    "✓ Evento de validación de driver publicado: DriverId={}, UserId={}",
                    evt.DriverId, evt.UserId
                );
            }

            await Task.CompletedTask;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "✗ Error publicando evento de validación de driver: DriverId={}", evt.DriverId);
            throw;
        }
    }
}
