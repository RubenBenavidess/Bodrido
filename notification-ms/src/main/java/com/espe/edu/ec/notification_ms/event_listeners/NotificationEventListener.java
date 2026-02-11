package com.espe.edu.ec.notification_ms.event_listeners;

import com.espe.edu.ec.notification_ms.config.RabbitMQConfig;
import com.espe.edu.ec.notification_ms.models.Notification;
import com.espe.edu.ec.notification_ms.repositories.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationRepository notificationRepository;

    /**
     * Escucha eventos de notificaciones ya construidas
     */
    @RabbitListener(queues = RabbitMQConfig.ORDERS_NOTIFICATIONS_QUEUE)
    @Transactional
    public void handleNotifications(OrderNotificationEvent event) {

        log.info(
            "Notificación recibida: entityType={}, entityId={}, action={}, severity={}",
            event.getEntityType(),
            event.getOrderId(),
            event.getAction(),
            event.getSeverity()
        );

        try {
            if (event.getEntityType() == null || event.getOrderId() == null) {
                log.warn("Notificación inválida, entityType u orderId nulos. Ignorando evento");
                return;
            }

            Notification notification = Notification.builder()
                    .id(event.getId())
                    .microservice(event.getMicroservice())
                    .action(event.getAction())
                    .entityType(event.getEntityType())
                    .entityId(event.getOrderId())
                    .message(event.getMessage())
                    .data(event.getData())
                    .severity(event.getSeverity())
                    .build();

            notificationRepository.save(notification);

            log.info(
                "Notificación guardada correctamente: entityId={}, action={}",
                event.getOrderId(),
                event.getAction()
            );

        } catch (Exception e) {
            log.error(
                "Error procesando notificación: entityId={}",
                event.getOrderId(),
                e
            );
        }
    }
}
