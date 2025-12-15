package com.espe.edu.ec.order_ms.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.espe.edu.ec.order_ms.models.Tariff;
import java.util.Optional;

import com.espe.edu.ec.order_ms.model_enums.VehicleType;


public interface TariffRepository extends JpaRepository<Tariff, Long>{
    Optional<Tariff> findByVehicleType(VehicleType vehicleType);
}
