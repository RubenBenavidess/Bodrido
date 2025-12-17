using FleetService.Data;
using FleetService.DTOs.Mappers;
using FleetService.DTOs.Request;
using FleetService.DTOs.Response;
using Microsoft.EntityFrameworkCore;

namespace FleetService.Services.Implementations
{
    public class VehicleService(FleetContext _context) : IVehicleService
    {
        public async Task<VehicleResponseDto> CreateVehicleAsync(VehicleRequestDto requestDto)
        {
            var vehicle = VehicleMapper.toEntity(requestDto);

            _context.Vehicles.Add(vehicle);

            await _context.SaveChangesAsync();

            return VehicleMapper.toDto(vehicle);
        }

        public async Task<VehicleResponseDto?> GetVehicleByIdAsync(Guid id)
        {
            var vehicle = await _context.Vehicles.FindAsync(id);
            return vehicle is null ? null : VehicleMapper.toDto(vehicle);
        }

        public async Task<IEnumerable<VehicleResponseDto>> GetAllVehiclesAsync()
        {
            var vehicles = await _context.Vehicles
                                            .AsNoTracking()
                                            .ToListAsync();

            return vehicles.Select(VehicleMapper.toDto);
        }
    }
}
