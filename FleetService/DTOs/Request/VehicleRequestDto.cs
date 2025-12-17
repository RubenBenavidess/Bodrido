using System.ComponentModel.DataAnnotations;
using System.Text.Json.Serialization;

namespace FleetService.DTOs.Request
{
    [JsonPolymorphic(TypeDiscriminatorPropertyName = "vehicleType")]
    [JsonDerivedType(typeof(MotorcycleRequestDto), typeDiscriminator: "MOTORCYCLE")]
    [JsonDerivedType(typeof(TruckRequestDto), typeDiscriminator: "TRUCK")]
    [JsonDerivedType(typeof(LightVehicleRequestDto), typeDiscriminator: "LIGHT_VEHICLE")]
    public class VehicleRequestDto
    {
        [Required]
        public required string Plate { get; set; }
        [Required]
        public required string Brand { get; set; }
        [Required]
        public required string Model { get; set; }
        [Required]
        public required decimal MaxLoadKg { get; set; }
        [Required]
        public required decimal VolumeM3 { get; set; }
        [Required]
        public required string CurrentZoneId { get; set; } 
    }
}
