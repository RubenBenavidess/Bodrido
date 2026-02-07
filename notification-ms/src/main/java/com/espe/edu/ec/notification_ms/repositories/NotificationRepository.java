package com.espe.edu.ec.notification_ms.repositories;

import com.espe.edu.ec.notification_ms.models.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    
    List<Notification> findByEntityTypeAndEntityId(String entityType, UUID entityId);
    
    List<Notification> findByMicroservice(String microservice);
    
    List<Notification> findByAction(String action);
    
    List<Notification> findByCreatedAtAfter(LocalDateTime createdAt);
    
    List<Notification> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, UUID entityId);
}
