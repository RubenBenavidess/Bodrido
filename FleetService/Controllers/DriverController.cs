using FleetService.DTOs.Request;
using FleetService.Models;
using FleetService.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace FleetService.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public class DriverController(IDriverService _driverService) : ControllerBase
    {
        [HttpGet]
        public async Task<IActionResult> GetAll([FromQuery] DriverStatus? status = null, [FromQuery] LicenseCategory? licenseCategory = null)
        {
            var drivers = await _driverService.GetAllDriversAsync(status, licenseCategory);
            return Ok(drivers);
        }

        [HttpPost]
        public async Task<IActionResult> RegisterDriver([FromBody] DriverRequestDto requestDto)
        {
            try
            {
                var createdDriver = await _driverService.RegisterDriverAsync(requestDto);

                return CreatedAtAction(
                    nameof(GetById),
                    new { id = createdDriver.Id },
                    createdDriver
                );
            }
            catch (ArgumentException ex)
            {
                return BadRequest(new { error = ex.Message });
            }
            catch (Exception ex)
            {
                return StatusCode(500, new { error = $"Error interno registrando el conductor: {ex.Message}" });
            }
        }

        [HttpGet("{id}")]
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
