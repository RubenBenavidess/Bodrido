using FleetService.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace FleetService.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    //[Authorize]
    public class VehicleController(IVehicleService _vehicleService) : ControllerBase
    {
        [HttpGet]
        public async Task<IActionResult> GetAll()
        {
            var vehicles = await _vehicleService.GetAllVehiclesAsync();
            return Ok(vehicles);
        }

        [HttpGet("{id}")]
        public async Task<IActionResult> GetById(Guid id)
        {
            var vehicle = await _vehicleService.GetVehicleByIdAsync(id);
            if (vehicle == null)
            {
                return NotFound();
            }
            return Ok(vehicle);
        }

        [HttpPost]
        //[Authorize(Policy = "RequireAdmin")]
        public async Task<IActionResult> Create([FromBody] DTOs.Request.VehicleRequestDto requestDto)
        {
            try
            {
                var createdVehicle = await _vehicleService.CreateVehicleAsync(requestDto);

                return CreatedAtAction(
                    nameof(GetById),
                    new { id = createdVehicle.Id },
                    createdVehicle
                );
            }
            catch (ArgumentException ex)
            {
                return BadRequest(new { error = ex.Message });
            }
            catch (Exception ex)
            {
                return StatusCode(500, new { error = $"Error interno creando el vehículo: {ex.Message}" });
            }
        }
    }
}
