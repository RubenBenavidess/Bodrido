namespace FleetService.Models
{
    public class LightVehicle : Vehicle
    {
        public int PassengerCapacity { get; set; }
        public bool HasAC { get; set; }

        public LightVehicle()
        {
            Type = VehicleType.LIGHT_VEHICLE;
        }

        public override bool ValidatePlate()
        {
            // Implementar logica real
            return !string.IsNullOrEmpty(Plate) && Plate.Length == 7;
        }
    }
}
