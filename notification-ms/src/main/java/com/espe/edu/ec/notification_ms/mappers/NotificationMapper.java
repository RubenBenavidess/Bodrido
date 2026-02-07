package com.espe.edu.ec.notification_ms.mappers;

import com.espe.edu.ec.notification_ms.dtos.NotificationResponse;
import com.espe.edu.ec.notification_ms.models.Notification;

public final class NotificationMapper {

    private NotificationMapper() {
        throw new UnsupportedOperationException("Clase utilitaria");
    }

    public static NotificationResponse entityToResponse(Notification notification) {
        if (notification == null) return null;

        return NotificationResponse.builder()
                .id(notification.getId())
                .microservice(notification.getMicroservice())
                .action(notification.getAction())
                .entityType(notification.getEntityType())
                .entityId(notification.getEntityId())
                .message(notification.getMessage())
                .data(notification.getData())
                .severity(notification.getSeverity())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
