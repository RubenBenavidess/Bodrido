import request from 'supertest';
import express from 'express';

/**
 * Pruebas de Autenticación - HTTP 401
 * 
 * Casos de prueba para el microservicio de autenticación:
 * - 401 UNAUTHORIZED: Usuario no autenticado (sin token o token inválido)
 * - Validación de estructura de tokens JWT
 * - Validación de claims en el token (role, scope, zone_id, fleet_type)
 */

// Mock inline del validador de tokens (no depende de archivos externos)
let mockValidateToken = null;

// Middleware de autenticación simulado
function requireAuth(req, res, next) {
  const authHeader = req.headers.authorization;
  
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({
      success: false,
      error: 'UNAUTHORIZED',
      message: 'Token de autenticación requerido'
    });
  }

  try {
    const token = authHeader.split(' ')[1];
    // Usar el mock en lugar del módulo real
    if (mockValidateToken) {
      const decoded = mockValidateToken(token);
      req.user = decoded;
      next();
    } else {
      throw new Error('No mock configured');
    }
  } catch (error) {
    return res.status(401).json({
      success: false,
      error: 'UNAUTHORIZED',
      message: 'Token inválido o expirado'
    });
  }
}

// App de prueba con rutas protegidas
function createTestApp() {
  const app = express();
  app.use(express.json());

  // Ruta pública (sin auth)
  app.get('/public', (req, res) => {
    res.json({ success: true, message: 'Endpoint público' });
  });

  // Ruta protegida - solo autenticados
  app.get('/protected', requireAuth, (req, res) => {
    res.json({ 
      success: true, 
      message: 'Acceso permitido',
      user: req.user 
    });
  });

  // Ruta para verificar claims del token
  app.get('/token/claims', requireAuth, (req, res) => {
    res.json({ 
      success: true, 
      claims: {
        user_id: req.user.user_id,
        username: req.user.username,
        role: req.user.role,
        scope: req.user.scope,
        zone_id: req.user.zone_id,
        fleet_type: req.user.fleet_type
      }
    });
  });

  return app;
}

describe('Authentication Tests - 401 UNAUTHORIZED', () => {
  let app;

  beforeEach(() => {
    app = createTestApp();
    mockValidateToken = null; // Reset mock
  });

  describe('401 UNAUTHORIZED - No Token or Invalid Token', () => {
    it('should allow access to public endpoint without token', async () => {
      const response = await request(app)
        .get('/public')
        .expect(200);

      expect(response.body.success).toBe(true);
      expect(response.body.message).toBe('Endpoint público');
    });

    it('should reject protected endpoint without token (401)', async () => {
      const response = await request(app)
        .get('/protected')
        .expect(401);

      expect(response.body.success).toBe(false);
      expect(response.body.error).toBe('UNAUTHORIZED');
      expect(response.body.message).toContain('Token de autenticación requerido');
    });

    it('should reject with invalid token format (401)', async () => {
      const response = await request(app)
        .get('/protected')
        .set('Authorization', 'InvalidFormat token123')
        .expect(401);

      expect(response.body.success).toBe(false);
      expect(response.body.error).toBe('UNAUTHORIZED');
    });

    it('should reject with expired token (401)', async () => {
      mockValidateToken = () => {
        const error = new Error('Token expired');
        error.name = 'TokenExpiredError';
        throw error;
      };

      const response = await request(app)
        .get('/protected')
        .set('Authorization', 'Bearer expired-token')
        .expect(401);

      expect(response.body.success).toBe(false);
      expect(response.body.error).toBe('UNAUTHORIZED');
      expect(response.body.message).toContain('inválido o expirado');
    });

    it('should reject with malformed token (401)', async () => {
      mockValidateToken = () => {
        throw new Error('Invalid token');
      };

      const response = await request(app)
        .get('/protected')
        .set('Authorization', 'Bearer malformed.token.here')
        .expect(401);

      expect(response.body.success).toBe(false);
    });
  });

  describe('Token Claims Validation', () => {
    it('should validate token contains required claims for CLIENT', async () => {
      mockValidateToken = () => ({
        user_id: 'client-456',
        username: 'client_user',
        role: 'CLIENT',
        scope: 'read:own_orders create:orders',
        zone_id: null,
        fleet_type: null
      });

      const response = await request(app)
        .get('/token/claims')
        .set('Authorization', 'Bearer valid-client-token')
        .expect(200);

      expect(response.body.success).toBe(true);
      expect(response.body.claims).toHaveProperty('user_id');
      expect(response.body.claims).toHaveProperty('username');
      expect(response.body.claims).toHaveProperty('role', 'CLIENT');
      expect(response.body.claims).toHaveProperty('scope');
    });

    it('should validate token contains required claims for DRIVER', async () => {
      mockValidateToken = () => ({
        user_id: 'driver-789',
        username: 'driver_user',
        role: 'DRIVER',
        scope: 'read:orders update:delivery_status',
        zone_id: 3,
        fleet_type: 'LIGHT_VEHICLE'
      });

      const response = await request(app)
        .get('/token/claims')
        .set('Authorization', 'Bearer valid-driver-token')
        .expect(200);

      expect(response.body.success).toBe(true);
      expect(response.body.claims.role).toBe('DRIVER');
      expect(response.body.claims.zone_id).toBe(3);
      expect(response.body.claims.fleet_type).toBe('LIGHT_VEHICLE');
      expect(response.body.claims.scope).toContain('read:orders');
    });

    it('should validate token contains required claims for ADMIN', async () => {
      mockValidateToken = () => ({
        user_id: 'admin-123',
        username: 'admin_user',
        role: 'ADMIN',
        scope: 'read:all write:all delete:all manage:users',
        zone_id: null,
        fleet_type: null
      });

      const response = await request(app)
        .get('/token/claims')
        .set('Authorization', 'Bearer valid-admin-token')
        .expect(200);

      expect(response.body.success).toBe(true);
      expect(response.body.claims.role).toBe('ADMIN');
      expect(response.body.claims.scope).toContain('read:all');
      expect(response.body.claims.scope).toContain('write:all');
    });

    it('should validate token contains required claims for SUPERVISOR', async () => {
      mockValidateToken = () => ({
        user_id: 'supervisor-456',
        username: 'supervisor_user',
        role: 'SUPERVISOR',
        scope: 'read:reports read:orders assign:drivers',
        zone_id: 5,
        fleet_type: null
      });

      const response = await request(app)
        .get('/token/claims')
        .set('Authorization', 'Bearer valid-supervisor-token')
        .expect(200);

      expect(response.body.success).toBe(true);
      expect(response.body.claims.role).toBe('SUPERVISOR');
      expect(response.body.claims.zone_id).toBe(5);
      expect(response.body.claims.scope).toContain('assign:drivers');
    });
  });

  describe('Token Structure Validation', () => {
    it('should ensure token has sub (username) claim', async () => {
      mockValidateToken = () => ({
        sub: 'test_user',
        user_id: 'uuid-123',
        username: 'test_user',
        role: 'CLIENT',
        scope: 'read:own_orders'
      });

      const response = await request(app)
        .get('/protected')
        .set('Authorization', 'Bearer valid-token')
        .expect(200);

      expect(response.body.user).toHaveProperty('sub', 'test_user');
      expect(response.body.user).toHaveProperty('username', 'test_user');
    });

    it('should validate scope format (space-separated permissions)', async () => {
      mockValidateToken = () => ({
        user_id: 'user-123',
        username: 'test_user',
        role: 'CLIENT',
        scope: 'read:own_orders create:orders update:own_profile'
      });

      const response = await request(app)
        .get('/token/claims')
        .set('Authorization', 'Bearer valid-token')
        .expect(200);

      const scopeArray = response.body.claims.scope.split(' ');
      expect(scopeArray).toBeInstanceOf(Array);
      expect(scopeArray.length).toBeGreaterThan(0);
      expect(scopeArray).toContain('read:own_orders');
    });

    it('should handle tokens with minimal claims (CLIENT without zone)', async () => {
      mockValidateToken = () => ({
        user_id: 'client-123',
        username: 'simple_client',
        role: 'CLIENT',
        scope: 'read:own_orders'
      });

      const response = await request(app)
        .get('/protected')
        .set('Authorization', 'Bearer minimal-token')
        .expect(200);

      expect(response.body.success).toBe(true);
      expect(response.body.user.role).toBe('CLIENT');
    });
  });
});
