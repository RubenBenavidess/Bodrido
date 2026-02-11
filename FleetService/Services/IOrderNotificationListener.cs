using System.Text;
using System.Text.Json;
using FleetService.Config;
using FleetService.Data;
using FleetService.Models;
using FleetService.Models.Events;
using Microsoft.EntityFrameworkCore;
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
            
            _logger.LogInformation("Mensaje recibido de order-ms: {}", message);
            
            var incomingEvent = JsonSerializer.Deserialize<IncomingOrderValidationRequest>(message, new JsonSerializerOptions
            {
                PropertyNameCaseInsensitive = true
            });
            
            if (incomingEvent == null || incomingEvent.OrderId == Guid.Empty)
            {
                _logger.LogWarning("No se pudo deserializar el evento de validación o OrderId es vacío");
                _channel?.BasicAck(ea.DeliveryTag, false);
                return;
            }

            _logger.LogInformation("Evento recibido: OrderId={}, DriverId={}, VehicleId={}, ValidationType={}", 
                incomingEvent.OrderId, incomingEvent.DriverId, incomingEvent.VehicleId, incomingEvent.ValidationType);

            // Dispatch por tipo de validación
            switch (incomingEvent.ValidationType?.ToUpper())
            {
                case "ASSIGNMENT_VALIDATION":
                    await ValidateAssignedResourcesAsync(incomingEvent);
                    break;
                case "CANCELLATION_VALIDATION":
                    await HandleCancellationValidationAsync(incomingEvent);
                    break;
                case "PICKUP_VALIDATION":
                    await HandlePickupValidationAsync(incomingEvent);
                    break;
                default:
                    _logger.LogWarning("ValidationType desconocido: {}. Intentando como ASSIGNMENT_VALIDATION.", 
                        incomingEvent.ValidationType);
                    await ValidateAssignedResourcesAsync(incomingEvent);
                    break;
            }

            _channel?.BasicAck(ea.DeliveryTag, false);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error procesando evento de validación");
            // Nack sin requeue para evitar loop infinito
            _channel?.BasicNack(ea.DeliveryTag, false, false);
        }
    }

    // ==================== ASSIGNMENT VALIDATION ====================

    private async Task ValidateAssignedResourcesAsync(IncomingOrderValidationRequest incomingEvent)
    {
        using var scope = _serviceProvider.CreateScope();
        var context = scope.ServiceProvider.GetRequiredService<FleetContext>();
        var producer = scope.ServiceProvider.GetRequiredService<IOrderValidationProducer>();

        var driverId = incomingEvent.DriverId;
        
        // vehicleId llega como string desde order-ms, parsearlo a Guid
        Guid vehicleId;
        if (!Guid.TryParse(incomingEvent.VehicleId, out vehicleId))
        {
            _logger.LogWarning("VehicleId inválido recibido: {}", incomingEvent.VehicleId);
            var failEvent = new OrderValidationEvent
            {
                OrderId = incomingEvent.OrderId,
                ValidationType = "resources_assigned",
                Success = false,
                ErrorMessage = $"VehicleId inválido: {incomingEvent.VehicleId}",
                Timestamp = DateTime.UtcNow.ToString("yyyy-MM-ddTHH:mm:ss")
            };
            await producer.PublishValidationAsync(failEvent);
            return;
        }

        var validationEvent = new OrderValidationEvent
        {
            OrderId = incomingEvent.OrderId,
            ValidationType = "resources_assigned",
            Timestamp = DateTime.UtcNow.ToString("yyyy-MM-ddTHH:mm:ss")
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
                        incomingEvent.OrderId, driverId, vehicleId);
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
            _logger.LogError(ex, "Error durante validación de recursos para OrderId={}", incomingEvent.OrderId);
        }

        // Publicar resultado de validación (éxito o fallo)
        await producer.PublishValidationAsync(validationEvent);
    }

    // ==================== CANCELLATION VALIDATION ====================

    /// <summary>
    /// Maneja la solicitud de cancelación desde order-ms.
    /// Libera el conductor (BUSY → AVAILABLE) y el vehículo (IsAssigned = false).
    /// Publica resultado de vuelta a order-ms.
    /// </summary>
    private async Task HandleCancellationValidationAsync(IncomingOrderValidationRequest incomingEvent)
    {
        _logger.LogInformation("► [CANCELLATION] Procesando solicitud de cancelación: OrderId={}, DriverId={}, VehicleId={}",
            incomingEvent.OrderId, incomingEvent.DriverId, incomingEvent.VehicleId);

        using var scope = _serviceProvider.CreateScope();
        var context = scope.ServiceProvider.GetRequiredService<FleetContext>();
        var producer = scope.ServiceProvider.GetRequiredService<IOrderValidationProducer>();

        var validationEvent = new OrderValidationEvent
        {
            OrderId = incomingEvent.OrderId,
            ValidationType = "cancellation_result",
            Timestamp = DateTime.UtcNow.ToString("yyyy-MM-ddTHH:mm:ss")
        };

        Guid vehicleId;
        var hasValidVehicleId = Guid.TryParse(incomingEvent.VehicleId, out vehicleId);

        using var transaction = await context.Database.BeginTransactionAsync();
        try
        {
            var driver = await context.Drivers.FindAsync(incomingEvent.DriverId);
            Vehicle? vehicle = hasValidVehicleId ? await context.Vehicles.FindAsync(vehicleId) : null;

            bool resourcesFreed = false;

            // Liberar conductor
            if (driver != null)
            {
                if (driver.Status == DriverStatus.BUSY)
                {
                    driver.Status = DriverStatus.AVAILABLE;
                    context.Drivers.Update(driver);
                    _logger.LogInformation("✓ [CANCELLATION] Conductor {} liberado (BUSY → AVAILABLE)", incomingEvent.DriverId);
                    resourcesFreed = true;
                }
                else
                {
                    _logger.LogWarning("⚠ [CANCELLATION] Conductor {} no estaba BUSY (estado: {}). Continuando.", 
                        incomingEvent.DriverId, driver.Status);
                    // Aún así permitimos la cancelación - el conductor podría haber sido liberado por otro proceso
                    resourcesFreed = true;
                }
            }
            else
            {
                _logger.LogWarning("⚠ [CANCELLATION] Conductor {} no encontrado. Continuando cancelación.", incomingEvent.DriverId);
                resourcesFreed = true; // Continuamos - el conductor podría haber sido eliminado
            }

            // Liberar vehículo
            if (vehicle != null)
            {
                if (vehicle.IsAssigned)
                {
                    vehicle.IsAssigned = false;
                    vehicle.UpdatedAt = DateTime.UtcNow;
                    context.Vehicles.Update(vehicle);
                    _logger.LogInformation("✓ [CANCELLATION] Vehículo {} liberado (IsAssigned = false)", vehicleId);
                }
                else
                {
                    _logger.LogWarning("⚠ [CANCELLATION] Vehículo {} ya no estaba asignado. Continuando.", vehicleId);
                }
            }
            else if (hasValidVehicleId)
            {
                _logger.LogWarning("⚠ [CANCELLATION] Vehículo {} no encontrado. Continuando cancelación.", vehicleId);
            }

            if (resourcesFreed)
            {
                await context.SaveChangesAsync();
                await transaction.CommitAsync();

                validationEvent.Success = true;
                _logger.LogInformation("✓ [CANCELLATION] Recursos liberados exitosamente para OrderId={}", incomingEvent.OrderId);
            }
            else
            {
                await transaction.RollbackAsync();
                validationEvent.Success = false;
                validationEvent.ErrorMessage = "No se pudieron liberar los recursos";
                _logger.LogError("✗ [CANCELLATION] No se pudieron liberar recursos para OrderId={}", incomingEvent.OrderId);
            }
        }
        catch (Exception ex)
        {
            await transaction.RollbackAsync();
            validationEvent.Success = false;
            validationEvent.ErrorMessage = $"Error liberando recursos: {ex.Message}";
            _logger.LogError(ex, "✗ [CANCELLATION] Error durante liberación de recursos para OrderId={}", incomingEvent.OrderId);
        }

        await producer.PublishValidationAsync(validationEvent);
    }

    // ==================== PICKUP VALIDATION ====================

    /// <summary>
    /// Maneja la solicitud de validación de pickup desde order-ms.
    /// Verifica que el conductor sigue BUSY y el vehículo sigue asignado.
    /// </summary>
    private async Task HandlePickupValidationAsync(IncomingOrderValidationRequest incomingEvent)
    {
        _logger.LogInformation("► [PICKUP] Procesando validación de pickup: OrderId={}, DriverId={}, VehicleId={}",
            incomingEvent.OrderId, incomingEvent.DriverId, incomingEvent.VehicleId);

        using var scope = _serviceProvider.CreateScope();
        var context = scope.ServiceProvider.GetRequiredService<FleetContext>();
        var producer = scope.ServiceProvider.GetRequiredService<IOrderValidationProducer>();

        var validationEvent = new OrderValidationEvent
        {
            OrderId = incomingEvent.OrderId,
            ValidationType = "pickup_result",
            Timestamp = DateTime.UtcNow.ToString("yyyy-MM-ddTHH:mm:ss")
        };

        Guid vehicleId;
        if (!Guid.TryParse(incomingEvent.VehicleId, out vehicleId))
        {
            validationEvent.Success = false;
            validationEvent.ErrorMessage = $"VehicleId inválido: {incomingEvent.VehicleId}";
            await producer.PublishValidationAsync(validationEvent);
            return;
        }

        try
        {
            // Validar conductor
            var driver = await context.Drivers.FindAsync(incomingEvent.DriverId);
            if (driver == null)
            {
                validationEvent.Success = false;
                validationEvent.ErrorMessage = $"Conductor no encontrado: {incomingEvent.DriverId}";
                _logger.LogWarning("✗ [PICKUP] Conductor no encontrado: {}", incomingEvent.DriverId);
            }
            else if (driver.Status != DriverStatus.BUSY)
            {
                validationEvent.Success = false;
                validationEvent.ErrorMessage = $"Conductor no está BUSY (estado: {driver.Status}). No puede hacer pickup.";
                _logger.LogWarning("✗ [PICKUP] Conductor {} no está BUSY (estado: {})", incomingEvent.DriverId, driver.Status);
            }
            else
            {
                // Validar vehículo
                var vehicle = await context.Vehicles.FindAsync(vehicleId);
                if (vehicle == null)
                {
                    validationEvent.Success = false;
                    validationEvent.ErrorMessage = $"Vehículo no encontrado: {vehicleId}";
                    _logger.LogWarning("✗ [PICKUP] Vehículo no encontrado: {}", vehicleId);
                }
                else if (!vehicle.IsAssigned)
                {
                    validationEvent.Success = false;
                    validationEvent.ErrorMessage = $"Vehículo no está asignado: {vehicleId}";
                    _logger.LogWarning("✗ [PICKUP] Vehículo {} no está asignado", vehicleId);
                }
                else if (vehicle.Condition != VehicleCondition.OPERATIONAL)
                {
                    validationEvent.Success = false;
                    validationEvent.ErrorMessage = $"Vehículo no operacional: {vehicle.Condition}";
                    _logger.LogWarning("✗ [PICKUP] Vehículo {} no operacional: {}", vehicleId, vehicle.Condition);
                }
                else
                {
                    // ✅ PICKUP VALIDADO: Conductor BUSY + Vehículo asignado + Operacional
                    validationEvent.Success = true;
                    _logger.LogInformation(
                        "✓ [PICKUP] Validación exitosa. Conductor BUSY, Vehículo asignado y operacional. OrderId={}",
                        incomingEvent.OrderId);
                }
            }
        }
        catch (Exception ex)
        {
            validationEvent.Success = false;
            validationEvent.ErrorMessage = $"Error validando pickup: {ex.Message}";
            _logger.LogError(ex, "✗ [PICKUP] Error durante validación de pickup para OrderId={}", incomingEvent.OrderId);
        }

        await producer.PublishValidationAsync(validationEvent);
    }
}
