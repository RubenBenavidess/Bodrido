using FleetService.DTOs.Request;
using FleetService.DTOs.Response;

namespace FleetService.Services
{
    public interface IVehicleService
    {
        Task<VehicleResponseDto> CreateVehicleAsync(VehicleRequestDto requestDto);
        Task<VehicleResponseDto?> GetVehicleByIdAsync(Guid id);
        Task<IEnumerable<VehicleResponseDto>> GetAllVehiclesAsync();
    }
}
