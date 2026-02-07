using RabbitMQ.Client;

namespace FleetService.Config;

public static class RabbitMQConfig
{
    // Notificaciones desde Order MS (ESCUCHAMOS)
    public const string NotificationExchangeName = "orders-notifications.exchange";
    public const string NotificationQueueName = "fleet-notifications.queue";
    public const string NotificationRoutingKey = "order.*";

    // Validaciones para Order MS (PUBLICAMOS)
    public const string ValidationExchangeName = "orders-validations.exchange";
    public const string ValidationQueueName = "orders-validations.queue";
    public const string ValidationRoutingKey = "validation.*";

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
