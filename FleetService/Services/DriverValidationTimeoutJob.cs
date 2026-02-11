using FleetService.Data;
using FleetService.Models;
using Microsoft.EntityFrameworkCore;

namespace FleetService.Services;

/// <summary>
/// Job schedulado que verifica drivers con timeout de validación.
/// Si un driver ha estado en WAITING_VERIFICATION por más de 30 segundos,
/// se marca como INACTIVE con estado VERIFICATION_TIMEOUT.
/// </summary>
public class DriverValidationTimeoutJob : BackgroundService
{
    private readonly IServiceProvider _serviceProvider;
    private readonly ILogger<DriverValidationTimeoutJob> _logger;
    private static readonly long TIMEOUT_SECONDS = 30;
    private static readonly int CHECK_INTERVAL_MS = 10000; // Verificar cada 10 segundos

    public DriverValidationTimeoutJob(
        IServiceProvider serviceProvider,
        ILogger<DriverValidationTimeoutJob> logger)
    {
        _serviceProvider = serviceProvider;
        _logger = logger;
    }

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        _logger.LogInformation("► DriverValidationTimeoutJob iniciado");

        while (!stoppingToken.IsCancellationRequested)
        {
            try
            {
                await CheckTimeoutDriversAsync();
                await Task.Delay(CHECK_INTERVAL_MS, stoppingToken);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "✗ Error en DriverValidationTimeoutJob");
            }
        }

        _logger.LogInformation("► DriverValidationTimeoutJob detenido");
    }

    private async Task CheckTimeoutDriversAsync()
    {
        using var scope = _serviceProvider.CreateScope();
        var context = scope.ServiceProvider.GetRequiredService<FleetContext>();

        try
        {
            // Buscar drivers en estado WAITING_VERIFICATION
            var timeoutThreshold = DateTime.UtcNow.AddSeconds(-TIMEOUT_SECONDS);

            var timeoutDrivers = await context.Drivers
                .Where(d => d.ValidationSagaStep == DriverValidationSagaStep.WAITING_VERIFICATION
                    && d.ValidationSagaStartedAt != null
                    && d.ValidationSagaStartedAt < timeoutThreshold)
                .ToListAsync();

            if (timeoutDrivers.Count == 0)
            {
                return; // Sin drivers con timeout
            }

            _logger.LogWarning(
                "⚠ Encontrados {} drivers con timeout de validación",
                timeoutDrivers.Count
            );

            using var transaction = await context.Database.BeginTransactionAsync();
            try
            {
                foreach (var driver in timeoutDrivers)
                {
                    _logger.LogWarning(
                        "► [TIMEOUT] Procesando timeout para DriverId={}, CreatedAt={}",
                        driver.Id, driver.CreatedAt
                    );

                    driver.ValidationSagaStep = DriverValidationSagaStep.VERIFICATION_TIMEOUT;
                    driver.Status = DriverStatus.INACTIVE;
                    driver.ValidationSagaReason = "Timeout de validación excedido (30 segundos)";
                    driver.IsValidationCompleted = true;
                    driver.UpdatedAt = DateTime.UtcNow;

                    _logger.LogInformation(
                        "✓ Driver marcado como timeout: DriverId={}, Status=INACTIVE",
                        driver.Id
                    );
                }

                await context.SaveChangesAsync();
                await transaction.CommitAsync();

                _logger.LogWarning(
                    "✓ Completado: {} drivers marcados como timeout",
                    timeoutDrivers.Count
                );
            }
            catch (Exception ex)
            {
                await transaction.RollbackAsync();
                _logger.LogError(ex, "Error descartando drivers por timeout");
                throw;
            }
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "✗ Error en CheckTimeoutDriversAsync");
        }
    }
}
