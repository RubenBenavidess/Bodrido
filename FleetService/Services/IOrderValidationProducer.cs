using System.Text.Json;
using FleetService.Config;
using FleetService.Models.Events;
using RabbitMQ.Client;

namespace FleetService.Services;

public interface IOrderValidationProducer
{
    Task PublishValidationAsync(OrderValidationEvent validationEvent);
}

public class OrderValidationProducer : IOrderValidationProducer
{
    private readonly IConnection _connection;
    private readonly ILogger<OrderValidationProducer> _logger;

    public OrderValidationProducer(IConnection connection, ILogger<OrderValidationProducer> logger)
    {
        _connection = connection;
        _logger = logger;
    }

    public async Task PublishValidationAsync(OrderValidationEvent validationEvent)
    {
        try
        {
            using var channel = _connection.CreateModel();
            
            var options = new JsonSerializerOptions
            {
                PropertyNamingPolicy = JsonNamingPolicy.CamelCase
            };
            var message = JsonSerializer.Serialize(validationEvent, options);
            var body = System.Text.Encoding.UTF8.GetBytes(message);

            var properties = channel.CreateBasicProperties();
            properties.Persistent = true;
            properties.ContentType = "application/json";
            properties.ContentEncoding = "UTF-8";
            properties.Priority = 0;
            
            // Spring AMQP requiere __TypeId__ para que Jackson2JsonMessageConverter
            // pueda deserializar el mensaje a la clase Java correcta
            properties.Headers = new Dictionary<string, object>
            {
                { "__TypeId__", "com.espe.edu.ec.order_ms.dtos.events.OrderValidationEvent" }
            };

            var routingKey = RabbitMQConfig.ValidationRoutingKey;
            
            channel.BasicPublish(
                exchange: RabbitMQConfig.ValidationExchangeName,
                routingKey: routingKey,
                basicProperties: properties,
                body: body);

            _logger.LogInformation("Validación publicada: OrderId={}, Success={}", 
                validationEvent.OrderId, validationEvent.Success);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error publicando validación para OrderId={}", validationEvent.OrderId);
            throw;
        }
    }
}
