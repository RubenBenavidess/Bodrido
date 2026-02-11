package com.espe.edu.ec.customer_ms.repositories;

import com.espe.edu.ec.customer_ms.models.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    Optional<Customer> findByUserId(UUID userId);
    Optional<Customer> findByEmail(String email);
    Optional<Customer> findByUsername(String username);
}
