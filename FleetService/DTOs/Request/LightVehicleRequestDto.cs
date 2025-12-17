namespace FleetService.DTOs.Request
{
    public class LightVehicleRequestDto : VehicleRequestDto
    {
        public int PassengerCapacity { get; set; }
        public bool HasAC { get; set; }
    }
}
