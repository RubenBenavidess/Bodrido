using System.Text;
using System.Text.Json;
using FleetService.Config;
using FleetService.Data;
using FleetService.Models;
using FleetService.Models.Events;
using RabbitMQ.Client;
using RabbitMQ.Client.Events;

namespace FleetService.Services;

public interface IOrderNotificationListener
{
    Task StartListeningAsync();
}

public class OrderNotificationListener : IOrderNotificationListener
{
    private readonly IConnection _connection;
    private readonly IServiceProvider _serviceProvider;
    private readonly ILogger<OrderNotificationListener> _logger;
    private IModel? _channel;

    public OrderNotificationListener(
        IConnection connection,
        IServiceProvider serviceProvider,
        ILogger<OrderNotificationListener> logger)
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

            var consumer = new AsyncEventingBasicConsumer(_channel);
            consumer.Received += HandleOrderEventAsync;

            _channel.BasicConsume(
                queue: RabbitMQConfig.NotificationQueueName,
                autoAck: false,
                consumer: consumer);

            _logger.LogInformation("Listener de notificaciones iniciado en queue: {}", 
                RabbitMQConfig.NotificationQueueName);
            
            await Task.CompletedTask;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error iniciando listener de notificaciones");
            throw;
        }
    }

    private async Task HandleOrderEventAsync(object model, BasicDeliverEventArgs ea)
    {
        try
        {
            var body = ea.Body.ToArray();
            var message = Encoding.UTF8.GetString(body);
            
            var notification = JsonSerializer.Deserialize<OrderNotificationEvent>(message);
            
            if (notification == null)
            {
                _logger.LogWarning("No se pudo deserializar notificación");
                _channel?.BasicAck(ea.DeliveryTag, false);
                return;
            }

            _logger.LogInformation("Notificación recibida: OrderId={}, Action={}", 
                notification.OrderId, notification.Action);

            if (notification.Action == "assigned")
            {
                await ValidateAssignedResourcesAsync(notification);
            }
            else if (notification.Action == "cancelled")
            {
                await HandleOrderCancelledAsync(notification);
            }

            _channel?.BasicAck(ea.DeliveryTag, false);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error procesando evento de notificación");
            // Nack sin requeue para evitar loop infinito
            _channel?.BasicNack(ea.DeliveryTag, false, false);
        }
    }

    private async Task ValidateAssignedResourcesAsync(OrderNotificationEvent notification)
    {
        using var scope = _serviceProvider.CreateScope();
        var context = scope.ServiceProvider.GetRequiredService<FleetContext>();
        var producer = scope.ServiceProvider.GetRequiredService<IOrderValidationProducer>();

        var driverId = notification.Data.TryGetValue("driverId", out var driverIdObj) 
            ? (Guid)driverIdObj 
            : Guid.Empty;

        var vehicleId = notification.Data.TryGetValue("vehicleId", out var vehicleIdObj) 
            ? (Guid)vehicleIdObj 
            : Guid.Empty;

        var validationEvent = new OrderValidationEvent
        {
            OrderId = notification.OrderId,
            ValidationType = "resources_assigned",
            Timestamp = DateTime.UtcNow
        };

        // Usar transacción para garantizar atomicidad
        using var transaction = await context.Database.BeginTransactionAsync();
        try
        {
            // Validar que el conductor existe
            var driver = await context.Drivers.FindAsync(driverId);
            if (driver == null)
            {
                validationEvent.Success = false;
                validationEvent.ErrorMessage = $"Conductor no encontrado: {driverId}";
                _logger.LogWarning("Validación fallida - Conductor no encontrado: {}", driverId);
            }
            // Validar que el conductor NO está ocupado (Status debe ser AVAILABLE)
            else if (driver.Status != DriverStatus.AVAILABLE)
            {
                validationEvent.Success = false;
                validationEvent.ErrorMessage = $"Conductor no disponible. Estado actual: {driver.Status}";
                _logger.LogWarning("Validación fallida - Conductor ocupado. DriverId={}, Status={}", driverId, driver.Status);
            }
            else
            {
                // Validar que el vehículo existe
                var vehicle = await context.Vehicles.FindAsync(vehicleId);
                if (vehicle == null)
                {
                    validationEvent.Success = false;
                    validationEvent.ErrorMessage = $"Vehículo no encontrado: {vehicleId}";
                    _logger.LogWarning("Validación fallida - Vehículo no encontrado: {}", vehicleId);
                }
                // Validar que el vehículo NO está asignado
                else if (vehicle.IsAssigned)
                {
                    validationEvent.Success = false;
                    validationEvent.ErrorMessage = $"Vehículo ya está asignado a otro pedido: {vehicleId}";
                    _logger.LogWarning("Validación fallida - Vehículo ya asignado. VehicleId={}", vehicleId);
                }
                // Validar que el vehículo está en condición operacional
                else if (vehicle.Condition != VehicleCondition.OPERATIONAL)
                {
                    validationEvent.Success = false;
                    validationEvent.ErrorMessage = $"Vehículo no operacional. Estado: {vehicle.Condition}";
                    _logger.LogWarning("Validación fallida - Vehículo no operacional. VehicleId={}, Condition={}", vehicleId, vehicle.Condition);
                }
                else
                {
                    // ✅ VALIDACIÓN EXITOSA: Actualizar estados de forma transaccional
                    driver.Status = DriverStatus.BUSY;
                    vehicle.IsAssigned = true;
                    vehicle.UpdatedAt = DateTime.UtcNow;

                    context.Drivers.Update(driver);
                    context.Vehicles.Update(vehicle);
                    await context.SaveChangesAsync();

                    validationEvent.Success = true;
                    _logger.LogInformation(
                        "Validación exitosa - Recursos asignados y actualizados. OrderId={}, DriverId={}, VehicleId={}", 
                        notification.OrderId, driverId, vehicleId);
                }
            }

            // Confirmar transacción solo si no hubo excepciones
            await transaction.CommitAsync();
        }
        catch (Exception ex)
        {
            // Revertir transacción en caso de error
            await transaction.RollbackAsync();
            
            validationEvent.Success = false;
            validationEvent.ErrorMessage = $"Error validando recursos: {ex.Message}";
            _logger.LogError(ex, "Error durante validación de recursos para OrderId={}", notification.OrderId);
        }

        // Publicar resultado de validación (éxito o fallo)
        await producer.PublishValidationAsync(validationEvent);
    }

    /// <summary>
    /// Maneja la cancelación de una orden y revierte los cambios en conductor y vehículo
    /// </summary>
    private async Task HandleOrderCancelledAsync(OrderNotificationEvent notification)
    {
        using var scope = _serviceProvider.CreateScope();
        var context = scope.ServiceProvider.GetRequiredService<FleetContext>();

        var driverId = notification.Data.TryGetValue("driverId", out var driverIdObj) 
            ? (Guid)driverIdObj 
            : Guid.Empty;

        var vehicleId = notification.Data.TryGetValue("vehicleId", out var vehicleIdObj) 
            ? (Guid)vehicleIdObj 
            : Guid.Empty;

        // Usar transacción para garantizar atomicidad
        using var transaction = await context.Database.BeginTransactionAsync();
        try
        {
            // Si los IDs son válidos, revertir los cambios
            if (driverId != Guid.Empty && vehicleId != Guid.Empty)
            {
                var driver = await context.Drivers.FindAsync(driverId);
                var vehicle = await context.Vehicles.FindAsync(vehicleId);

                if (driver != null)
                {
                    driver.Status = DriverStatus.AVAILABLE;
                    context.Drivers.Update(driver);
                    _logger.LogInformation("Conductor {} revertido a AVAILABLE por cancelación de orden {}", 
                        driverId, notification.OrderId);
                }

                if (vehicle != null)
                {
                    vehicle.IsAssigned = false;
                    vehicle.UpdatedAt = DateTime.UtcNow;
                    context.Vehicles.Update(vehicle);
                    _logger.LogInformation("Vehículo {} revertido a disponible por cancelación de orden {}", 
                        vehicleId, notification.OrderId);
                }

                if (driver != null || vehicle != null)
                {
                    await context.SaveChangesAsync();
                }
            }

            // Confirmar transacción
            await transaction.CommitAsync();
            
            _logger.LogInformation("Orden {} cancelada exitosamente. Recursos liberados.", 
                notification.OrderId);
        }
        catch (Exception ex)
        {
            // Revertir transacción en caso de error
            await transaction.RollbackAsync();
            
            _logger.LogError(ex, 
                "Error revirtiendo asignación de recursos para orden cancelada: OrderId={}", 
                notification.OrderId);
            throw;
        }
    }
}
