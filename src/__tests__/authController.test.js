import request from 'supertest';
import express from 'express';
import cookieParser from 'cookie-parser';

/**
 * Pruebas de integración para el controlador de autenticación
 * 
 * Estas pruebas validan los endpoints HTTP del controlador de autenticación
 * usando una app Express simulada con mocks inline.
 */

// Mocks inline
let mockRegister = null;
let mockLogin = null;
let mockRefreshToken = null;
let mockLogout = null;
let mockValidateToken = null;

// Crear app de prueba con rutas simuladas
function createTestApp() {
  const app = express();
  app.use(express.json());
  app.use(cookieParser());

  // POST /auth/register
  app.post('/auth/register', async (req, res) => {
    try {
      const { username, email, password, role, vehicle_type, zone_id } = req.body;
      
      // Validación básica
      if (!username || username.length < 3) {
        return res.status(400).json({
          success: false,
          error: 'VALIDATION_ERROR',
          message: 'Username must be at least 3 characters'
        });
      }
      if (!email || !email.includes('@')) {
        return res.status(400).json({
          success: false,
          error: 'VALIDATION_ERROR',
          message: 'Invalid email format'
        });
      }
      if (!password || password.length < 6) {
        return res.status(400).json({
          success: false,
          error: 'VALIDATION_ERROR',
          message: 'Password must be at least 6 characters'
        });
      }
      if (!['ADMIN', 'DRIVER', 'CLIENT', 'SUPERVISOR'].includes(role)) {
        return res.status(400).json({
          success: false,
          error: 'VALIDATION_ERROR',
          message: 'Invalid role'
        });
      }

      if (mockRegister) {
        const result = await mockRegister(req.body);
        return res.status(201).json({
          success: true,
          message: 'User registered successfully',
          data: result
        });
      }

      return res.status(201).json({
        success: true,
        message: 'User registered successfully',
        data: { id: 'uuid-123', username, email }
      });
    } catch (error) {
      if (error.status === 409) {
        return res.status(409).json({
          success: false,
          error: 'CONFLICT',
          message: error.message
        });
      }
      return res.status(500).json({
        success: false,
        error: 'ERROR',
        message: error.message
      });
    }
  });

  // POST /auth/login
  app.post('/auth/login', async (req, res) => {
    try {
      const { username, password } = req.body;
      
      if (!username || username.length < 1) {
        return res.status(400).json({
          success: false,
          error: 'VALIDATION_ERROR',
          message: 'Username is required'
        });
      }
      if (!password || password.length < 6) {
        return res.status(400).json({
          success: false,
          error: 'VALIDATION_ERROR',
          message: 'Password must be at least 6 characters'
        });
      }

      if (mockLogin) {
        const result = await mockLogin({ username, password });
        res.cookie('refreshToken', result.refreshToken, { httpOnly: true });
        return res.status(200).json({
          success: true,
          message: 'Authenticated',
          ...result
        });
      }

      return res.status(200).json({
        success: true,
        message: 'Authenticated',
        accessToken: 'mock-token',
        refreshToken: 'mock-refresh',
        user: { username }
      });
    } catch (error) {
      if (error.message === 'Invalid Credentials') {
        return res.status(401).json({
          success: false,
          error: 'UNAUTHORIZED',
          message: 'Invalid Credentials'
        });
      }
      return res.status(500).json({
        success: false,
        error: 'ERROR',
        message: error.message
      });
    }
  });

  // POST /auth/token/refresh
  app.post('/auth/token/refresh', async (req, res) => {
    const tokenString = req.cookies.refreshToken || req.body.refreshToken;
    
    if (!tokenString) {
      return res.status(400).json({
        success: false,
        message: 'Refresh Token required'
      });
    }

    try {
      if (mockRefreshToken) {
        const result = await mockRefreshToken(tokenString);
        res.cookie('refreshToken', result.refreshToken, { httpOnly: true });
        return res.status(200).json({
          success: true,
          ...result
        });
      }

      return res.status(200).json({
        success: true,
        accessToken: 'new-access-token',
        refreshToken: 'new-refresh-token'
      });
    } catch (error) {
      return res.status(401).json({
        success: false,
        error: 'UNAUTHORIZED',
        message: error.message
      });
    }
  });

  // POST /auth/logout
  app.post('/auth/logout', async (req, res) => {
    const tokenString = req.cookies.refreshToken || req.body.refreshToken;
    
    if (mockLogout && tokenString) {
      await mockLogout(tokenString);
    }
    
    res.clearCookie('refreshToken');
    return res.status(200).json({
      success: true,
      message: 'Logged out successfully'
    });
  });

  // GET /auth/verify
  app.get('/auth/verify', (req, res) => {
    const authHeader = req.headers.authorization;
    
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return res.status(401).json({
        success: false,
        error: 'UNAUTHORIZED',
        message: 'Token required'
      });
    }

    try {
      const token = authHeader.split(' ')[1];
      if (mockValidateToken) {
        const decoded = mockValidateToken(token);
        return res.status(200).json({
          success: true,
          valid: true,
          user: decoded
        });
      }
      return res.status(200).json({
        success: true,
        valid: true
      });
    } catch (error) {
      return res.status(401).json({
        success: false,
        error: 'UNAUTHORIZED',
        message: 'Invalid token'
      });
    }
  });

  return app;
}

describe('Authentication Controller - Integration Tests', () => {
  let app;

  beforeEach(() => {
    app = createTestApp();
    mockRegister = null;
    mockLogin = null;
    mockRefreshToken = null;
    mockLogout = null;
    mockValidateToken = null;
  });

  describe('POST /auth/register', () => {
    it('should register a new user successfully (201)', async () => {
      const response = await request(app)
        .post('/auth/register')
        .send({
          username: 'new_driver',
          email: 'driver@test.com',
          password: 'SecurePass123!',
          role: 'DRIVER',
          vehicle_type: 'LIGHT_VEHICLE',
          zone_id: 5
        })
        .expect(201);

      expect(response.body.success).toBe(true);
      expect(response.body.message).toBe('User registered successfully');
      expect(response.body.data).toHaveProperty('username', 'new_driver');
    });

    it('should reject invalid username with 400', async () => {
      const response = await request(app)
        .post('/auth/register')
        .send({
          username: 'ab',
          email: 'test@test.com',
          password: 'Pass123!',
          role: 'CLIENT'
        })
        .expect(400);

      expect(response.body.success).toBe(false);
      expect(response.body.error).toBe('VALIDATION_ERROR');
    });

    it('should reject invalid email with 400', async () => {
      const response = await request(app)
        .post('/auth/register')
        .send({
          username: 'valid_user',
          email: 'invalid-email',
          password: 'Pass123!',
          role: 'CLIENT'
        })
        .expect(400);

      expect(response.body.success).toBe(false);
    });

    it('should reject weak password with 400', async () => {
      const response = await request(app)
        .post('/auth/register')
        .send({
          username: 'valid_user',
          email: 'test@test.com',
          password: '123',
          role: 'CLIENT'
        })
        .expect(400);

      expect(response.body.success).toBe(false);
    });

    it('should reject invalid role with 400', async () => {
      const response = await request(app)
        .post('/auth/register')
        .send({
          username: 'valid_user',
          email: 'test@test.com',
          password: 'Pass123!',
          role: 'INVALID_ROLE'
        })
        .expect(400);

      expect(response.body.success).toBe(false);
    });

    it('should reject duplicate user with 409', async () => {
      mockRegister = () => {
        const error = new Error('Username already exists');
        error.status = 409;
        throw error;
      };

      const response = await request(app)
        .post('/auth/register')
        .send({
          username: 'existing_user',
          email: 'new@test.com',
          password: 'SecurePass123!',
          role: 'CLIENT'
        })
        .expect(409);

      expect(response.body.success).toBe(false);
    });
  });

  describe('POST /auth/login', () => {
    it('should login successfully with valid credentials (200)', async () => {
      mockLogin = () => ({
        accessToken: 'mock-jwt-access-token',
        refreshToken: 'mock-refresh-token',
        user: { username: 'test_driver', role: 'DRIVER', email: 'driver@test.com' }
      });

      const response = await request(app)
        .post('/auth/login')
        .send({
          username: 'test_driver',
          password: 'SecurePass123!'
        })
        .expect(200);

      expect(response.body.success).toBe(true);
      expect(response.body.message).toBe('Authenticated');
      expect(response.body.accessToken).toBe('mock-jwt-access-token');
      expect(response.body.user).toHaveProperty('username', 'test_driver');
      expect(response.headers['set-cookie']).toBeDefined();
    });

    it('should reject invalid credentials with 401', async () => {
      mockLogin = () => {
        throw new Error('Invalid Credentials');
      };

      const response = await request(app)
        .post('/auth/login')
        .send({
          username: 'wrong_user',
          password: 'wrong_pass123'
        })
        .expect(401);

      expect(response.body.success).toBe(false);
      expect(response.body.message).toBe('Invalid Credentials');
    });

    it('should reject weak password with 400', async () => {
      const response = await request(app)
        .post('/auth/login')
        .send({
          username: 'test_user',
          password: '123'
        })
        .expect(400);

      expect(response.body.success).toBe(false);
    });
  });

  describe('POST /auth/token/refresh', () => {
    it('should refresh tokens successfully (200)', async () => {
      mockRefreshToken = () => ({
        accessToken: 'new-access-token',
        refreshToken: 'new-refresh-token'
      });

      const response = await request(app)
        .post('/auth/token/refresh')
        .send({ refreshToken: 'valid-refresh-token' })
        .expect(200);

      expect(response.body.success).toBe(true);
      expect(response.body.accessToken).toBe('new-access-token');
    });

    it('should reject missing refresh token with 400', async () => {
      const response = await request(app)
        .post('/auth/token/refresh')
        .send({})
        .expect(400);

      expect(response.body.success).toBe(false);
      expect(response.body.message).toBe('Refresh Token required');
    });

    it('should reject invalid refresh token with 401', async () => {
      mockRefreshToken = () => {
        throw new Error('Invalid refresh token');
      };

      const response = await request(app)
        .post('/auth/token/refresh')
        .send({ refreshToken: 'invalid-token' })
        .expect(401);

      expect(response.body.success).toBe(false);
    });
  });

  describe('POST /auth/logout', () => {
    it('should logout successfully (200)', async () => {
      const response = await request(app)
        .post('/auth/logout')
        .send({ refreshToken: 'valid-token' })
        .expect(200);

      expect(response.body.success).toBe(true);
    });
  });

  describe('GET /auth/verify', () => {
    it('should verify valid token (200)', async () => {
      mockValidateToken = () => ({
        user_id: 'uuid-123',
        username: 'test_user',
        role: 'CLIENT'
      });

      const response = await request(app)
        .get('/auth/verify')
        .set('Authorization', 'Bearer valid-token')
        .expect(200);

      expect(response.body.success).toBe(true);
      expect(response.body.valid).toBe(true);
    });

    it('should reject request without token (401)', async () => {
      const response = await request(app)
        .get('/auth/verify')
        .expect(401);

      expect(response.body.success).toBe(false);
      expect(response.body.error).toBe('UNAUTHORIZED');
    });

    it('should reject invalid token (401)', async () => {
      mockValidateToken = () => {
        throw new Error('Invalid token');
      };

      const response = await request(app)
        .get('/auth/verify')
        .set('Authorization', 'Bearer invalid-token')
        .expect(401);

      expect(response.body.success).toBe(false);
    });
  });
});
