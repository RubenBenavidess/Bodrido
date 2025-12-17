using FleetService.Data;
using FleetService.DTOs.Mappers;
using FleetService.DTOs.Request;
using FleetService.DTOs.Response;
using FleetService.Models;
using Microsoft.EntityFrameworkCore;

namespace FleetService.Services.Implementations
{
    public class DriverService(FleetContext _context) : IDriverService
    {
        public async Task<DriverResponseDto> RegisterDriverAsync(DriverRequestDto request)
        {
            bool alreadyExists = await _context.Drivers
                .AnyAsync(d => d.UserId == request.UserId);

            if (alreadyExists)
            {
                throw new InvalidOperationException($"El usuario {request.UserId} ya está registrado como conductor.");
            }

            var driver = DriverMapper.ToEntity(request);

            _context.Drivers.Add(driver);
            await _context.SaveChangesAsync();

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
            await _context.SaveChangesAsync();

            return DriverMapper.ToDto(driver);
        }
    }
}
