package com.espe.edu.ec.notification_ms.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketJwtInterceptor implements ChannelInterceptor {

    private final JwtProvider jwtProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            // Extraer el token del encabezado 'Authorization' o de un parámetro 'token'
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            String token = null;

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            } else {
                // Alternativa: por si Postman/Frontend lo envía como query param
                token = accessor.getFirstNativeHeader("token");
            }

            if (token != null && jwtProvider.validateToken(token)) {
                log.info("WebSocket connection authenticated for user: {}", jwtProvider.getUsername(token));
                
                // Opcional: Establecer la autenticación en el contexto del mensaje
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        jwtProvider.getUsername(token), null, null);
                accessor.setUser(auth);
            } else {
                log.error("WebSocket connection rejected: Invalid or missing JWT");
                throw new IllegalArgumentException("Unauthorized: Invalid Token");
            }
        }
        return message;
    }
}