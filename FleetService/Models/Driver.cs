using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace FleetService.Models
{
    public class Driver
    {
        [Key]
        public Guid Id { get; set; } = Guid.NewGuid();

        [Required]
        public required Guid UserId { get; set; }

        [Required]
        [MaxLength(20)]
        public required string LicenseNumber { get; set; }

        [Required]
        [MaxLength(2)]
        public required string LicenseCategory { get; set; }

        public Guid? CurrentVehicleId { get; set; }

        [ForeignKey("CurrentVehicleId")]
        public Vehicle? CurrentVehicle { get; set; }

        public DriverStatus Status { get; set; } = DriverStatus.OFF_DUTY;

        [Column(TypeName = "jsonb")]
        public Location LastLocation { get; set; } = new Location();
    }
}
