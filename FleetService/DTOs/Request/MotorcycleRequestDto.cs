namespace FleetService.DTOs.Request
{
    public class MotorcycleRequestDto : VehicleRequestDto
    {
        public int HelmetCount { get; set; }
        public string BoxDimensions { get; set; } = string.Empty;
    }
}