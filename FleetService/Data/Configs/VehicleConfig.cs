using FleetService.Models;
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;

namespace FleetService.Data.Configs
{
    public class VehicleConfig : IEntityTypeConfiguration<Vehicle>
    {
        public void Configure(EntityTypeBuilder<Vehicle> builder)
        {
            builder.HasKey(v => v.Id);

            builder.HasIndex(v => v.Plate)
                   .IsUnique();

            builder.Property(v => v.MaxLoadKg)
                   .HasPrecision(10, 2);

            builder.Property(v => v.VolumeM3)
                   .HasPrecision(10, 2);

            builder.HasDiscriminator<VehicleType>("Type")
                   .HasValue<Motorcycle>(VehicleType.MOTORCYCLE)
                   .HasValue<LightVehicle>(VehicleType.LIGHT_VEHICLE)
                   .HasValue<Truck>(VehicleType.TRUCK);
        }
    }
}
