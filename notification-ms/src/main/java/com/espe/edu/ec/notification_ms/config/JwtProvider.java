package com.espe.edu.ec.notification_ms.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtProvider {

    private final JwtDecoder jwtDecoder;

    /**
     * Valida el token usando el JwtDecoder configurado con la llave pública EC.
     */
    public boolean validateToken(String token) {
        try {
            jwtDecoder.decode(token);
            return true;
        } catch (JwtException e) {
            log.error("Token JWT inválido o expirado: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Obtiene el username (subject) del token.
     */
    public String getUsername(String token) {
        Jwt jwt = jwtDecoder.decode(token);
        return jwt.getSubject(); // El campo 'sub' que envías desde Node.js
    }

    /**
     * Ejemplo de cómo obtener claims personalizados que definiste en Node.js
     * (role, zone_id, fleet_type, etc.)
     */
    public Object getClaim(String token, String claimName) {
        Jwt jwt = jwtDecoder.decode(token);
        return jwt.getClaim(claimName);
    }
}