namespace FleetService.Models
{
    public class Motorcycle : Vehicle
    {
        public int HelmetCount { get; set; }
        public string BoxDimensions { get; set; } = string.Empty;

        public Motorcycle()
        {
            Type = VehicleType.MOTORCYCLE;
        }

        public override bool ValidatePlate()
        {
            // Implementar logica real
            return !string.IsNullOrEmpty(Plate) && Plate.Length <= 7;
        }
    }
}
