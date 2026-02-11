using FleetService.DTOs.Request;
using FleetService.Models;
using FleetService.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace FleetService.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    [Authorize]
    public class DriverController(IDriverService _driverService) : ControllerBase
    {
        [HttpGet]
        [Authorize(Policy = "FleetView")]
        public async Task<IActionResult> GetAll([FromQuery] DriverStatus? status = null, [FromQuery] LicenseCategory? licenseCategory = null)
        {
            var drivers = await _driverService.GetAllDriversAsync(status, licenseCategory);
            return Ok(drivers);
        }

        /// <summary>
        /// Configura un conductor existente (creado automáticamente al registrarse en auth-ms).
        /// Solo actualiza licencia y categoría. No permite crear conductores nuevos.
        /// </summary>
        [HttpPatch("{id}/configure")]
        [Authorize(Policy = "FleetUpdate")]
        public async Task<IActionResult> ConfigureDriver(Guid id, [FromBody] DriverConfigureDto configDto)
        {
            try
            {
                var updatedDriver = await _driverService.ConfigureDriverAsync(id, configDto);
                return Ok(updatedDriver);
            }
            catch (KeyNotFoundException ex)
            {
                return NotFound(new { error = ex.Message });
            }
            catch (Exception ex)
            {
                return StatusCode(500, new { error = $"Error interno configurando el conductor: {ex.Message}" });
            }
        }

        [HttpGet("{id}")]
        [Authorize(Policy = "FleetView")]
        public async Task<IActionResult> GetById(Guid id)
        {
            var driver = await _driverService.GetDriverByIdAsync(id);
            if (driver == null)
            {
                return NotFound();
            }
            return Ok(driver);
        }

        [HttpGet("user/{userId}")]
        [Authorize(Policy = "FleetView")]
        public async Task<IActionResult> GetByUserId(Guid userId)
        {
            var driver = await _driverService.GetDriverByUserIdAsync(userId);
            if (driver == null)
            {
                return NotFound();
            }
            return Ok(driver);
        }

        [HttpPatch("{id}/status")]
        [Authorize(Policy = "FleetUpdate")]
        public async Task<IActionResult> UpdateStatus(Guid id, [FromBody] UpdateDriverStatusDto requestDto)
        {
            try
            {
                var updatedDriver = await _driverService.UpdateDriverStatusAsync(id, requestDto.Status);
                if (updatedDriver == null)
                {
                    return NotFound();
                }
                return Ok(updatedDriver);
            }
            catch (ArgumentException ex)
            {
                return BadRequest(new { error = ex.Message });
            }
            catch (Exception ex)
            {
                return StatusCode(500, new { error = $"Error interno actualizando el estado: {ex.Message}" });
            }
        }
    }
}
