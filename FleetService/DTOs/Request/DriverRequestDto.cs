using FleetService.Models;
using System.ComponentModel.DataAnnotations;

namespace FleetService.DTOs.Request
{
    public class DriverRequestDto
    {
        [Required]
        public required Guid UserId { get; set; }

        [Required]
        public required string LicenseNumber { get; set; }

        [Required]
        public required LicenseCategory LicenseCategory { get; set; }
    }
}
