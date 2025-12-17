using FleetService.Models;
using System.Text.Json.Serialization;

namespace FleetService.DTOs.Response
{
    public class VehicleResponseDto
    {
        public required Guid Id { get; set; }
        public VehicleType Type { get; set; }
        public required string Plate { get; set; }
        public required string Brand { get; set; }
        public required string Model { get; set; }
        public required decimal MaxLoadKg { get; set; }
        public required decimal VolumeM3 { get; set; }
        public required string CurrentZoneId { get; set; }
        public required VehicleCondition Condition { get; set; }
        public required bool IsAssigned { get; set; }
        public required Dictionary<string, object> Features { get; set; }
    }
}
