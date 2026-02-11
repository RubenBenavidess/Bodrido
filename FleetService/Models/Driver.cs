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
        public required LicenseCategory LicenseCategory { get; set; }

        public Guid? CurrentVehicleId { get; set; }

        [ForeignKey("CurrentVehicleId")]
        public Vehicle? CurrentVehicle { get; set; }

        public DriverStatus Status { get; set; } = DriverStatus.OFF_DUTY;

        [Column(TypeName = "jsonb")]
        public Location LastLocation { get; set; } = new Location();

        // ==================== SAGA VALIDATION FIELDS ====================
        /// <summary>
        /// Estado de la saga de validación del conductor.
        /// Rastrea si el usuario existe en auth-ms.
        /// </summary>
        [Column("validation_saga_step")]
        public DriverValidationSagaStep ValidationSagaStep { get; set; } = DriverValidationSagaStep.WAITING_VERIFICATION;

        /// <summary>
        /// Razón de rechazo en caso de que la validación falle.
        /// Ej: "User not found", "User is inactive", "Timeout"
        /// </summary>
        [MaxLength(500)]
        [Column("validation_saga_reason")]
        public string? ValidationSagaReason { get; set; }

        /// <summary>
        /// Momento en que se inicia la saga de validación.
        /// Se usa para detectar timeouts (30 segundos).
        /// </summary>
        [Column("validation_saga_started_at")]
        public DateTime? ValidationSagaStartedAt { get; set; }

        /// <summary>
        /// Indicador de validación completada (éxito o fallo).
        /// Se usa para saber si la saga terminó.
        /// </summary>
        [Column("is_validation_completed")]
        public bool IsValidationCompleted { get; set; } = false;

        /// <summary>
        /// Timestamp de creación del conductor.
        /// </summary>
        [Column("created_at")]
        public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

        /// <summary>
        /// Timestamp de última actualización.
        /// </summary>
        [Column("updated_at")]
        public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;
    }
}
