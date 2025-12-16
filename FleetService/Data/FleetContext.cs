using FleetService.Models;
using Microsoft.EntityFrameworkCore;
using System.ComponentModel.DataAnnotations;
using System.Reflection;

namespace FleetService.Data
{
    public class FleetContext : DbContext
    {
        public FleetContext(DbContextOptions<FleetContext> options) : base(options) { }

        public DbSet<Vehicle> Vehicles { get; set; }
        
        public DbSet<Motorcycle> Motorcycles { get; set; }

        protected override void OnModelCreating(ModelBuilder modelBuilder)
        {
            base.OnModelCreating(modelBuilder);

            modelBuilder.ApplyConfigurationsFromAssembly(Assembly.GetExecutingAssembly());
        }

        public override Task<int> SaveChangesAsync(CancellationToken cancellationToken = default)
        {
            var vehiclesToCheck = ChangeTracker.Entries<Vehicle>()
                .Where(e => e.State == EntityState.Added || e.State == EntityState.Modified);

            foreach (var entry in vehiclesToCheck)
            {
                if (!entry.Entity.ValidatePlate())
                {
                    throw new ValidationException(
                        $"La placa '{entry.Entity.Plate}' no es válida para {entry.Entity.Type}."
                    );
                }
            }

            return base.SaveChangesAsync(cancellationToken);
        }
    }
}
