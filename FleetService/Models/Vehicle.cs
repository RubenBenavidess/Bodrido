using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;
using System.Text.Json.Serialization;

namespace FleetService.Models
{
    [JsonPolymorphic(TypeDiscriminatorPropertyName = "type")]
    [JsonDerivedType(typeof(Motorcycle), typeDiscriminator: "MOTORCYCLE")]
    [JsonDerivedType(typeof(LightVehicle), typeDiscriminator: "LIGHT_VEHICLE")]
    [JsonDerivedType(typeof(Truck), typeDiscriminator: "TRUCK")]
    public abstract class Vehicle
    {
        [Key]
        public Guid Id { get; set; } = Guid.NewGuid();

        [Required]
        [MaxLength(20)]
        public string Plate { get; set; } = string.Empty;

        [MaxLength(50)]
        public string Brand { get; set; } = string.Empty;

        [MaxLength(50)]
        public string Model { get; set; } = string.Empty;

        public VehicleType Type { get; set; }

        public VehicleCondition Condition { get; set; } = VehicleCondition.OPERATIONAL;

        public bool IsAssigned { get; set; } = false;

        [Column(TypeName = "decimal(10, 2)")]
        public decimal MaxLoadKg { get; set; }

        [Column(TypeName = "decimal(10, 2)")]
        public decimal VolumeM3 { get; set; }

        [MaxLength(50)]
        public string CurrentZoneId { get; set; } = string.Empty;

        public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
        public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;


        public abstract bool ValidatePlate();
    }
}
