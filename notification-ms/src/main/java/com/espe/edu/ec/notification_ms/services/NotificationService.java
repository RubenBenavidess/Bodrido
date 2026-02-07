package com.espe.edu.ec.notification_ms.services;

import com.espe.edu.ec.notification_ms.dtos.NotificationResponse;
import com.espe.edu.ec.notification_ms.mappers.NotificationMapper;
import com.espe.edu.ec.notification_ms.repositories.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsByEntity(String entityType, UUID entityId) {
        log.info("Obteniendo notificaciones para: entityType={}, entityId={}", entityType, entityId);
        
        return notificationRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId)
                .stream()
                .map(NotificationMapper::entityToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsByMicroservice(String microservice) {
        log.info("Obteniendo notificaciones de: microservice={}", microservice);
        
        return notificationRepository.findByMicroservice(microservice)
                .stream()
                .map(NotificationMapper::entityToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getAllNotifications() {
        log.info("Obteniendo todas las notificaciones");
        
        return notificationRepository.findAll()
                .stream()
                .map(NotificationMapper::entityToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public NotificationResponse getNotificationById(UUID id) {
        return notificationRepository.findById(id)
                .map(NotificationMapper::entityToResponse)
                .orElseThrow(() -> new IllegalArgumentException("Notificación no encontrada: " + id));
    }
}
