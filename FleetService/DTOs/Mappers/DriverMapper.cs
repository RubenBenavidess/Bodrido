using FleetService.DTOs.Request;
using FleetService.DTOs.Response;
using FleetService.Models;

namespace FleetService.DTOs.Mappers
{
    public class DriverMapper
    {
        public static Driver ToEntity(DriverRequestDto dto)
        {
            return new Driver
            {
                UserId = dto.UserId,
                LicenseNumber = dto.LicenseNumber,
                LicenseCategory = dto.LicenseCategory,
            };
        }

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