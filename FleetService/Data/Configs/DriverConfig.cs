using FleetService.Models;
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;

namespace FleetService.Data.Configs
{
    public class DriverConfig : IEntityTypeConfiguration<Driver>
    {
        public void Configure(EntityTypeBuilder<Driver> builder)
        {
            builder.HasKey(d => d.Id);

            builder.HasIndex(d => d.UserId)
                   .IsUnique();

            builder.Property(d => d.LicenseNumber)
                   .IsRequired()
                   .HasMaxLength(20);

            builder.Property(d => d.LastLocation)
                   .HasColumnType("jsonb");
        }
    }
}
