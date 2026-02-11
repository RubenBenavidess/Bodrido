package com.espe.edu.ec.notification_ms.controllers;

import com.espe.edu.ec.notification_ms.dtos.NotificationResponse;
import com.espe.edu.ec.notification_ms.services.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notificaciones", description = "Historial de eventos de órdenes y facturas")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;
    
    /**
     * Obtiene todas las notificaciones (requiere scope: order:view)
     */
    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_order:view')")
    @Operation(summary = "Listar todas las notificaciones", description = "Obtiene el historial completo de notificaciones. Requiere scope: order:view")
    public ResponseEntity<List<NotificationResponse>> getAllNotifications() {
        List<NotificationResponse> notifications = notificationService.getAllNotifications();
        return ResponseEntity.ok(notifications);
    }

    /**
     * Obtiene una notificación por ID (requiere scope: order:view)
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_order:view')")
    @Operation(summary = "Obtener notificación por ID", description = "Obtiene los detalles de una notificación específica. Requiere scope: order:view")
    public ResponseEntity<NotificationResponse> getNotificationById(@PathVariable UUID id) {
        NotificationResponse notification = notificationService.getNotificationById(id);
        return ResponseEntity.ok(notification);
    }

    /**
     * Obtiene notificaciones de una entidad específica (ORDER, INVOICE, etc.)
     * GET /api/v1/notifications/entity/ORDER/{entityId}
     * Requiere scope: order:view
     */
    @GetMapping("/entity/{entityType}/{entityId}")
    @PreAuthorize("hasAuthority('SCOPE_order:view')")
    @Operation(summary = "Obtener notificaciones por entidad", description = "Obtiene todas las notificaciones asociadas a una orden o factura. Requiere scope: order:view")
    public ResponseEntity<List<NotificationResponse>> getNotificationsByEntity(
            @PathVariable String entityType,
            @PathVariable UUID entityId) {
        List<NotificationResponse> notifications = notificationService.getNotificationsByEntity(entityType, entityId);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Obtiene notificaciones de un microservicio específico
     * GET /api/v1/notifications/microservice/billing-ms
     * Requiere scope: order:view
     */
    @GetMapping("/microservice/{microservice}")
    @PreAuthorize("hasAuthority('SCOPE_order:view')")
    @Operation(summary = "Obtener notificaciones por microservicio", description = "Obtiene las notificaciones generadas por un microservicio específico. Requiere scope: order:view")
    public ResponseEntity<List<NotificationResponse>> getNotificationsByMicroservice(
            @PathVariable String microservice) {
        List<NotificationResponse> notifications = notificationService.getNotificationsByMicroservice(microservice);
        return ResponseEntity.ok(notifications);
    }
}
