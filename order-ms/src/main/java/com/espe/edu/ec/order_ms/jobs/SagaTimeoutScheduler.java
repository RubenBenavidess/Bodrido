package com.espe.edu.ec.order_ms.jobs;

import com.espe.edu.ec.order_ms.models.Order;
import com.espe.edu.ec.order_ms.repositories.OrderRepository;
import com.espe.edu.ec.order_ms.services.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class SagaTimeoutScheduler {
    
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    
    // Timeout de 30 segundos (30000 ms)
    private static final long TIMEOUT_SECONDS = 30;
    
    /**
     * Job schedulado que se ejecuta cada 10 segundos para detectar sagas con timeout.
     * Elimina órdenes que han estado esperando verificación por más de 30 segundos.
     */
    @Scheduled(fixedDelay = 10000)
    public void checkTimeoutSagas() {
        try {
            log.info("► [TIMEOUT-JOB] Iniciando verificación de sagas con timeout...");
            
            LocalDateTime timeoutThreshold = LocalDateTime.now().minusSeconds(TIMEOUT_SECONDS);
            
            // Buscar órdenes pendientes creadas hace más de TIMEOUT_SECONDS
            List<Order> timeoutOrders = orderRepository.findTimeoutPendingOrders(timeoutThreshold);
            
            if (timeoutOrders.isEmpty()) {
                log.debug("✓ [TIMEOUT-JOB] No hay órdenes con timeout");
                return;
            }
            
            log.warn("⚠ [TIMEOUT-JOB] Encontradas {} órdenes con timeout", timeoutOrders.size());
            
            for (Order order : timeoutOrders) {
                try {
                    log.info("► [TIMEOUT-JOB] Procesando timeout para orderId={}, createdAt={}", 
                            order.getId(), order.getCreatedAt());
                    
                    orderService.dropOrderDueToTimeout(order.getId());
                    
                    log.info("✓ [TIMEOUT-JOB] Orden descartada por timeout: orderId={}", order.getId());
                } catch (Exception e) {
                    log.error("✗ [TIMEOUT-JOB] Error descartando orden por timeout: orderId={}", 
                            order.getId(), e);
                }
            }
        }catch(Exception e){
            log.error("✗ [TIMEOUT-JOB] Error en job de timeout de sagas de verificación de customer", e);
        }
    }
    /**
     * Job schedulado que se ejecuta cada 10 segundos para detectar sagas de asignación con timeout.
     * Retorna órdenes a CREATED si han estado esperando validación de Fleet por más de 30 segundos.
     */
    @Scheduled(fixedDelay = 10000)
    public void checkAssignmentTimeoutSagas() {
        try {
            log.info("► [ASSIGNMENT-TIMEOUT-JOB] Iniciando verificación de sagas de asignación con timeout...");
            
            LocalDateTime timeoutThreshold = LocalDateTime.now().minusSeconds(TIMEOUT_SECONDS);
            
            // Buscar órdenes en ASSIGNMENT_PENDING creadas hace más de TIMEOUT_SECONDS
            List<Order> assignmentTimeoutOrders = orderRepository.findTimeoutAssignmentOrders(timeoutThreshold);
            
            if (assignmentTimeoutOrders.isEmpty()) {
                log.debug("✓ [ASSIGNMENT-TIMEOUT-JOB] No hay órdenes con timeout de asignación");
                return;
            }
            
            log.warn("⚠ [ASSIGNMENT-TIMEOUT-JOB] Encontradas {} órdenes con timeout de asignación", assignmentTimeoutOrders.size());
            
            for (Order order : assignmentTimeoutOrders) {
                try {
                    log.info("► [ASSIGNMENT-TIMEOUT-JOB] Procesando timeout de asignación para orderId={}, assignmentSagaStartedAt={}", 
                            order.getId(), order.getAssignmentSagaStartedAt());
                    
                    orderService.rejectAssignmentAndReturnToCreated(order.getId(), "Timeout en validación de asignación de FleetService");
                    
                    log.info("✓ [ASSIGNMENT-TIMEOUT-JOB] Orden revertida a CREATED por timeout: orderId={}", order.getId());
                } catch (Exception e) {
                    log.error("✗ [ASSIGNMENT-TIMEOUT-JOB] Error revirtiendo orden por timeout de asignación: orderId={}", 
                            order.getId(), e);
                }
            }
            
        } catch (Exception e) {
            log.error("✗ [ASSIGNMENT-TIMEOUT-JOB] Error en job de timeout de sagas de asignación", e);
        }
    }
}