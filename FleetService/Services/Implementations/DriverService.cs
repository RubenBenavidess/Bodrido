using FleetService.Data;
using FleetService.DTOs.Mappers;
using FleetService.DTOs.Request;
using FleetService.DTOs.Response;
using FleetService.Models;
using FleetService.Models.Events;
using Microsoft.EntityFrameworkCore;

namespace FleetService.Services.Implementations
{
    public class DriverService(
        FleetContext _context,
        IDriverValidationProducer _validationProducer,
        ILogger<DriverService> _logger) : IDriverService
    {
        /// <summary>
        /// Configura un conductor existente (creado automáticamente por el evento de auth-ms).
        /// Actualiza licencia y categoría, y opcionalmente inicia la saga de validación.
        /// </summary>
        public async Task<DriverResponseDto> ConfigureDriverAsync(Guid driverId, DriverConfigureDto configDto)
        {
            var driver = await _context.Drivers
                .Include(d => d.CurrentVehicle)
                .FirstOrDefaultAsync(d => d.Id == driverId);

            if (driver == null)
            {
                throw new KeyNotFoundException(
                    $"No se encontró el conductor con Id={driverId}. " +
                    "Los conductores son creados automáticamente al registrarse en auth-ms."
                );
            }

            // Actualizar datos de licencia
            driver.LicenseNumber = configDto.LicenseNumber;
            driver.LicenseCategory = configDto.LicenseCategory;
            driver.UpdatedAt = DateTime.UtcNow;

            await _context.SaveChangesAsync();

            _logger.LogInformation(
                "✓ Conductor configurado: DriverId={}, LicenseNumber={}, LicenseCategory={}",
                driver.Id, driver.LicenseNumber, driver.LicenseCategory
            );

            return DriverMapper.ToDto(driver);
        }

        public async Task<DriverResponseDto?> GetDriverByIdAsync(Guid id)
        {
            var driver = await _context.Drivers
                .Include(d => d.CurrentVehicle)
                .FirstOrDefaultAsync(d => d.Id == id);

            return driver == null ? null : DriverMapper.ToDto(driver);
        }

        public async Task<DriverResponseDto?> GetDriverByUserIdAsync(Guid userId)
        {
            var driver = await _context.Drivers
                .Include(d => d.CurrentVehicle)
                .FirstOrDefaultAsync(d => d.UserId == userId);

            return driver == null ? null : DriverMapper.ToDto(driver);
        }

        public async Task<IEnumerable<DriverResponseDto>> GetAllDriversAsync(DriverStatus? status = null, LicenseCategory? licenseCategory = null)
        {
            var query = _context.Drivers
                .Include(d => d.CurrentVehicle)
                .AsQueryable();

            if (status.HasValue)
            {
                query = query.Where(d => d.Status == status.Value);
            }

            if (licenseCategory.HasValue)
            {
                query = query.Where(d => d.LicenseCategory == licenseCategory.Value);
            }

            var drivers = await query.AsNoTracking().ToListAsync();

            return drivers.Select(DriverMapper.ToDto);
        }

        public async Task<DriverResponseDto?> UpdateDriverStatusAsync(Guid driverId, DriverStatus newStatus)
        {
            var driver = await _context.Drivers
                .Include(d => d.CurrentVehicle)
                .FirstOrDefaultAsync(d => d.Id == driverId);

            if (driver == null)
            {
                return null;
            }

            driver.Status = newStatus;
            driver.UpdatedAt = DateTime.UtcNow;
            await _context.SaveChangesAsync();

            return DriverMapper.ToDto(driver);
        }
    }
}
