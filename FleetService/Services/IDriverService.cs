using FleetService.DTOs.Request;
using FleetService.DTOs.Response;
using FleetService.Models;

namespace FleetService.Services
{
    public interface IDriverService
    {
        /// <summary>
        /// Configura un conductor existente (creado por auth-ms) con su licencia.
        /// </summary>
        Task<DriverResponseDto> ConfigureDriverAsync(Guid driverId, DriverConfigureDto configDto);

        Task<DriverResponseDto?> GetDriverByIdAsync(Guid id);
        Task<DriverResponseDto?> GetDriverByUserIdAsync(Guid userId);
        Task<IEnumerable<DriverResponseDto>> GetAllDriversAsync(DriverStatus? status = null, LicenseCategory? licenseCategory = null);
        Task<DriverResponseDto?> UpdateDriverStatusAsync(Guid driverId, DriverStatus newStatus);
    }
}
