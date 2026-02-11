package com.espe.edu.ec.order_ms.repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.espe.edu.ec.order_ms.models.Order;

public interface OrderRepository extends JpaRepository<Order, UUID> {
       List<Order> findByCustomerId(UUID customerId);

       @Query("SELECT o FROM Order o WHERE o.sagaStep = 'WAITING_VERIFICATION' " +
                     "AND o.sagaStartedAt < :timeout")
       List<Order> findTimeoutPendingOrders(@Param("timeout") LocalDateTime timeout);

       @Query("SELECT o FROM Order o WHERE o.sagaStep = 'WAITING_VERIFICATION'")
       List<Order> findPendingVerificationOrders();

       @Query("SELECT o FROM Order o WHERE o.status = 'ASSIGNMENT_PENDING' " +
                     "AND o.assignmentSagaStartedAt < :timeout")
       List<Order> findTimeoutAssignmentOrders(@Param("timeout") LocalDateTime timeout);

       @Query("SELECT o FROM Order o WHERE o.status = 'ASSIGNMENT_PENDING'")
       List<Order> findPendingAssignmentOrders();

       @Query("SELECT o FROM Order o WHERE o.status = 'CANCELLATION_PENDING' " +
                     "AND o.cancellationSagaStartedAt < :timeout")
       List<Order> findTimeoutCancellationOrders(@Param("timeout") LocalDateTime timeout);

       @Query("SELECT o FROM Order o WHERE o.status = 'PICKUP_PENDING' " +
                     "AND o.pickupSagaStartedAt < :timeout")
       List<Order> findTimeoutPickupOrders(@Param("timeout") LocalDateTime timeout);
}
