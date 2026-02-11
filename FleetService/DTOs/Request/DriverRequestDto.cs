using FleetService.Models;
using System.ComponentModel.DataAnnotations;

namespace FleetService.DTOs.Request
{
    /// <summary>
    /// DTO para configurar un conductor ya creado desde auth-ms.
    /// No incluye UserId porque el driver ya existe en la DB.
    /// </summary>
    public class DriverConfigureDto
    {
        [Required]
        public required string LicenseNumber { get; set; }

        [Required]
        public required LicenseCategory LicenseCategory { get; set; }
    }
}
