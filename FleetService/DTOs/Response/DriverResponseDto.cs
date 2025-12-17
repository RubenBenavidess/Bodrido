using FleetService.Models;

namespace FleetService.DTOs.Response
{
    public class DriverResponseDto
    {
        public required Guid Id { get; set; }
        public required Guid UserId { get; set; }
        public required string LicenseNumber { get; set; }
        public required DriverStatus Status { get; set; }
        public Guid? CurrentVehicleId { get; set; }
        public string? CurrentVehiclePlate { get; set; }
    }
}
