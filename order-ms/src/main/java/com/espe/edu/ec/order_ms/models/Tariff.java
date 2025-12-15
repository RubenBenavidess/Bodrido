package com.espe.edu.ec.order_ms.models;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

import com.espe.edu.ec.order_ms.model_enums.VehicleType;

@Entity
@Table(name = "tariffs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tariff {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "zone_id", nullable = false)
    private String zoneId;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false)
    private VehicleType vehicleType;

    @Column(name = "base_cost", precision = 19, scale = 4, nullable = false)
    private BigDecimal baseCost;

    @Column(name = "cost_per_km", precision = 19, scale = 4, nullable = false)
    private BigDecimal costPerKm;

    @Column(name = "cost_per_kg", precision = 19, scale = 4)
    private BigDecimal costPerKg;

    @Column(name = "min_distance_km", nullable = false)
    private Integer minDistanceKm;
}
