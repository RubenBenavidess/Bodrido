using RabbitMQ.Client;

namespace FleetService.Config;

public static class RabbitMQConfig
{
    // Notificaciones desde Order MS (ESCUCHAMOS validaciones de asignación)
    // order-ms publica en: orders-validations-fleet.exchange con routing: orders.validations.fleet
    public const string NotificationExchangeName = "orders-validations-fleet.exchange";
    public const string NotificationQueueName = "fleet-notifications.queue";
    public const string NotificationRoutingKey = "orders.validations.fleet";

    // Validaciones para Order MS (PUBLICAMOS resultado de vuelta)
    // order-ms escucha en: order-fleet.verification.result queue, exchange: order-fleet-verification-result.exchange
    public const string ValidationExchangeName = "order-fleet-verification-result.exchange";
    public const string ValidationQueueName = "order-fleet.verification.result";
    public const string ValidationRoutingKey = "order-fleet.verification.result";

    public static void ConfigureRabbitMQ(this WebApplicationBuilder builder)
    {
        var rabbitMQConfig = builder.Configuration.GetSection("RabbitMQ");
        var hostName = rabbitMQConfig["HostName"] ?? "localhost";
        var userName = rabbitMQConfig["UserName"] ?? "guest";
        var password = rabbitMQConfig["Password"] ?? "guest";

        var factory = new ConnectionFactory
        {
            HostName = hostName,
            UserName = userName,
            Password = password,
            DispatchConsumersAsync = true
        };

        var connection = factory.CreateConnection();
        var channel = connection.CreateModel();

        // Configurar exchange y queue para notificaciones (escuchamos)
        channel.ExchangeDeclare(
            exchange: NotificationExchangeName,
            type: ExchangeType.Topic,
            durable: true);

        channel.QueueDeclare(
            queue: NotificationQueueName,
            durable: true,
            exclusive: false,
            autoDelete: false);

        channel.QueueBind(
            queue: NotificationQueueName,
            exchange: NotificationExchangeName,
            routingKey: NotificationRoutingKey);

        // Configurar exchange y queue para validaciones (publicamos)
        channel.ExchangeDeclare(
            exchange: ValidationExchangeName,
            type: ExchangeType.Topic,
            durable: true);

        channel.QueueDeclare(
            queue: ValidationQueueName,
            durable: true,
            exclusive: false,
            autoDelete: false);

        channel.QueueBind(
            queue: ValidationQueueName,
            exchange: ValidationExchangeName,
            routingKey: ValidationRoutingKey);

        builder.Services.AddSingleton(connection);
        builder.Services.AddSingleton(channel);
    }
}
