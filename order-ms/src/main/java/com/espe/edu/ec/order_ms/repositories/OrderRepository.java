package com.espe.edu.ec.order_ms.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.espe.edu.ec.order_ms.models.Order;

public interface OrderRepository extends JpaRepository<Order, UUID>{
    List<Order> findByCustomerId(UUID customerId);
}
