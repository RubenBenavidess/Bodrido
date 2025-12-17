namespace FleetService.Models
{
    public class Truck : Vehicle
    {
        public int NumberOfAxles { get; set; }
        public bool HasRefrigeration { get; set; }

        public Truck()
        {
            Type = VehicleType.TRUCK;
        }

        public override bool ValidatePlate()
        {
            // Implementar logica real
            return !string.IsNullOrEmpty(Plate) && Plate.Contains("-");
        }
    }
}
