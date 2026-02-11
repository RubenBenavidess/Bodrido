// ============================================================
//  Middleware de Autenticación JWT (ES256 / ECDSA)
//
//  Replica el mismo mecanismo de los otros microservicios:
//  - order-ms (Java): SecurityConfig.java con EC public key
//  - FleetService (.NET): JwtMiddleware.cs con EC public key
//  - auth-ms (Node): jwtManager.js firma con ES256
//
//  La clave pública se carga desde:
//  1. Variable de entorno JWT_PUBLIC_KEY_PATH (ruta al .pem)
//  2. Variable de entorno JWT_PUBLIC_KEY (contenido PEM inline)
//  3. Fallback: ../auth-ms/keys/ec_public.pem (desarrollo local)
// ============================================================

const jwt = require('jsonwebtoken');
const fs = require('fs');
const path = require('path');

let PUBLIC_KEY = null;

/**
 * Carga la clave pública EC para validar tokens JWT.
 * Se ejecuta una sola vez al iniciar el servidor.
 */
function loadPublicKey() {
    // Opción 1: ruta al archivo PEM
    if (process.env.JWT_PUBLIC_KEY_PATH) {
        try {
            PUBLIC_KEY = fs.readFileSync(process.env.JWT_PUBLIC_KEY_PATH, 'utf8');
            console.log('[Auth] ✅ Clave pública cargada desde:', process.env.JWT_PUBLIC_KEY_PATH);
            return;
        } catch (err) {
            console.error('[Auth] ❌ Error cargando clave desde JWT_PUBLIC_KEY_PATH:', err.message);
        }
    }

    // Opción 2: contenido PEM inline en variable de entorno
    if (process.env.JWT_PUBLIC_KEY) {
        PUBLIC_KEY = process.env.JWT_PUBLIC_KEY;
        console.log('[Auth] ✅ Clave pública cargada desde variable JWT_PUBLIC_KEY');
        return;
    }

    // Opción 3: fallback para desarrollo local
    const fallbackPath = path.resolve(__dirname, '..', '..', '..', 'auth-ms', 'keys', 'ec_public.pem');
    try {
        PUBLIC_KEY = fs.readFileSync(fallbackPath, 'utf8');
        console.log('[Auth] ✅ Clave pública cargada desde fallback:', fallbackPath);
    } catch (err) {
        console.error('[Auth] ⚠️  No se encontró clave pública. El servicio arrancará SIN autenticación.');
        console.error('[Auth]    Configura JWT_PUBLIC_KEY_PATH o JWT_PUBLIC_KEY');
    }
}

/**
 * Valida un token JWT usando la clave pública EC (ES256).
 * Retorna el payload decodificado o null si es inválido.
 *
 * @param {string} token - Token JWT (sin "Bearer ")
 * @returns {object|null} - Payload decodificado o null
 */
function verifyToken(token) {
    if (!PUBLIC_KEY) return null;

    try {
        const decoded = jwt.verify(token, PUBLIC_KEY, {
            algorithms: ['ES256'], // Mismo algoritmo que auth-ms
        });
        return decoded;
    } catch (err) {
        console.warn('[Auth] Token inválido:', err.message);
        return null;
    }
}

/**
 * Extrae y valida el token del header Authorization.
 * Retorna { user, token } o lanza un error GraphQL.
 *
 * @param {import('http').IncomingMessage} req
 * @returns {{ user: object, token: string }}
 */
function authenticateRequest(req) {
    const authHeader = req.headers.authorization;

    if (!authHeader) {
        throw new AuthenticationError('Se requiere el header Authorization');
    }

    // Soportar "Bearer <token>" o solo "<token>"
    const token = authHeader.startsWith('Bearer ')
        ? authHeader.slice(7)
        : authHeader;

    if (!token) {
        throw new AuthenticationError('Token vacío');
    }

    const user = verifyToken(token);

    if (!user) {
        throw new AuthenticationError('Token inválido o expirado');
    }

    return { user, token: authHeader };
}

/**
 * Error personalizado de autenticación.
 * Se traduce a un error GraphQL con extensions.code = 'UNAUTHENTICATED'.
 */
class AuthenticationError extends Error {
    constructor(message) {
        super(message);
        this.name = 'AuthenticationError';
        this.extensions = {
            code: 'UNAUTHENTICATED',
            http: { status: 401 },
        };
    }
}

// Cargar la clave al importar el módulo
loadPublicKey();

module.exports = {
    authenticateRequest,
    verifyToken,
    loadPublicKey,
    AuthenticationError,
};
