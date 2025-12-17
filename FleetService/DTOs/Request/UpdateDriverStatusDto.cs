using FleetService.Models;
using System.ComponentModel.DataAnnotations;

namespace FleetService.DTOs.Request
{
    public class UpdateDriverStatusDto
    {
        [Required]
        public required DriverStatus Status { get; set; }
    }
}
