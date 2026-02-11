using FleetService.DTOs.Request;
using FleetService.DTOs.Response;
using FleetService.Models;

namespace FleetService.DTOs.Mappers
{
    public class DriverMapper
    {
        public static DriverResponseDto ToDto(Driver entity)
        {
            return new DriverResponseDto
            {
                Id = entity.Id,
                UserId = entity.UserId,
                LicenseNumber = entity.LicenseNumber,
                LicenseCategory = entity.LicenseCategory,
                Status = entity.Status,
                CurrentVehicleId = entity.CurrentVehicleId,
                CurrentVehiclePlate = entity.CurrentVehicle?.Plate
            };
        }
    }
}