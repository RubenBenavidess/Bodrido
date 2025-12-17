using FleetService.DTOs.Request;
using FleetService.DTOs.Response;
using FleetService.Models;

namespace FleetService.DTOs.Mappers
{
    public class VehicleMapper
    {
        public static Vehicle toEntity(VehicleRequestDto dto)
        {
            Vehicle vehicle = dto switch
            {
                MotorcycleRequestDto motorcycleDto => new Motorcycle
                {
                    HelmetCount = motorcycleDto.HelmetCount,
                    BoxDimensions = motorcycleDto.BoxDimensions,
                    Type = VehicleType.MOTORCYCLE
                },

                TruckRequestDto truckDto => new Truck
                {
                    NumberOfAxles = truckDto.NumberOfAxles,
                    HasRefrigeration = truckDto.HasRefrigeration,
                    Type = VehicleType.TRUCK
                },

                LightVehicleRequestDto lightVehicleDto => new LightVehicle
                {
                    PassengerCapacity = lightVehicleDto.PassengerCapacity,
                    HasAC = lightVehicleDto.HasAC,
                    Type = VehicleType.LIGHT_VEHICLE
                },
                _ => throw new ArgumentException($"Tipo de DTO de vehículo no soportado: {dto.GetType().Name}", nameof(dto))
            };

            vehicle.Plate = dto.Plate;
            vehicle.Brand = dto.Brand;
            vehicle.Model = dto.Model;
            vehicle.MaxLoadKg = dto.MaxLoadKg;
            vehicle.VolumeM3 = dto.VolumeM3;
            vehicle.CurrentZoneId = dto.CurrentZoneId;

            return vehicle;
        }

        public static VehicleResponseDto toDto(Vehicle entity)
        {
            var dto = new VehicleResponseDto
            {
                Id = entity.Id,
                Type = entity.Type,
                Plate = entity.Plate,
                Brand = entity.Brand,
                Model = entity.Model,
                MaxLoadKg = entity.MaxLoadKg,
                VolumeM3 = entity.VolumeM3,
                CurrentZoneId = entity.CurrentZoneId,
                Condition = entity.Condition,
                IsAssigned = entity.IsAssigned,
                Features = new Dictionary<string, object>()
            };

            addFeaturesToDto(entity, dto);

            return dto;
        }

        private static void addFeaturesToDto(Vehicle entity, VehicleResponseDto dto)
        {
            switch (entity)
            {
                case Motorcycle m:
                    dto.Features.Add("helmetCount", m.HelmetCount);
                    dto.Features.Add("boxDimensions", m.BoxDimensions);
                    break;

                case LightVehicle l:
                    dto.Features.Add("passengerCapacity", l.PassengerCapacity);
                    dto.Features.Add("hasAC", l.HasAC);
                    break;

                case Truck t:
                    dto.Features.Add("numberOfAxles", t.NumberOfAxles);
                    dto.Features.Add("hasRefrigeration", t.HasRefrigeration);
                    break;
            }
        }

    }
}