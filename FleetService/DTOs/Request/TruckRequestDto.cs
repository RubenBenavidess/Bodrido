namespace FleetService.DTOs.Request
{
    public class TruckRequestDto : VehicleRequestDto
    {
        public int NumberOfAxles { get; set; }
        public bool HasRefrigeration { get; set; }
    }
}
