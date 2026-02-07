package com.espe.edu.ec.notification_ms.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {

    private UUID id;
    private String microservice;
    private String action;
    private String entityType;
    private UUID entityId;
    private String message;
    private String data;
    private String severity;
    private LocalDateTime createdAt;
}
