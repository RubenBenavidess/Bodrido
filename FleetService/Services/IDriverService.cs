using FleetService.DTOs.Request;
using FleetService.DTOs.Response;
using FleetService.Models;

namespace FleetService.Services
{
    public interface IDriverService
    {
        Task<DriverResponseDto> RegisterDriverAsync(DriverRequestDto requestDto);
        Task<DriverResponseDto?> GetDriverByIdAsync(Guid id);
        Task<DriverResponseDto?> GetDriverByUserIdAsync(Guid userId);
        Task<DriverResponseDto?> UpdateDriverStatusAsync(Guid driverId, DriverStatus newStatus);
    }
}
