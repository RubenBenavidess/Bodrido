/**
 * Setup global para tests
 * Configuración de mocks y helpers comunes
 */

// Mock global de variables de entorno - NO cargar keys reales
process.env.NODE_ENV = 'test';

// Helper para crear tokens mock
export function createMockToken(payload = {}) {
  return {
    user_id: payload.user_id || 'test-user-id',
    username: payload.username || 'test_user',
    role: payload.role || 'CLIENT',
    scope: payload.scope || 'read:own_orders create:orders',
    zone_id: payload.zone_id || null,
    fleet_type: payload.fleet_type || null,
    iat: Math.floor(Date.now() / 1000),
    exp: Math.floor(Date.now() / 1000) + 3600
  };
}

// Helper para crear usuarios mock
export function createMockUser(overrides = {}) {
  return {
    id: overrides.id || 'uuid-test-123',
    username: overrides.username || 'test_user',
    email: overrides.email || 'test@example.com',
    password_hash: overrides.password_hash || '$2b$10$hashedpassword',
    role_id: overrides.role_id || 1,
    vehicle_type: overrides.vehicle_type || null,
    zone_id: overrides.zone_id || null,
    is_active: overrides.is_active !== undefined ? overrides.is_active : true,
    created_at: overrides.created_at || new Date(),
    Role: overrides.Role || {
      id: 1,
      name: 'CLIENT',
      Permissions: [
        { id: 1, slug: 'read:own_orders' },
        { id: 2, slug: 'create:orders' }
      ]
    },
    Zone: overrides.Zone || null,
    ...overrides
  };
}

// Helper para crear roles mock
export function createMockRole(name = 'CLIENT', permissions = []) {
  const roleMap = {
    CLIENT: { id: 1, name: 'CLIENT' },
    DRIVER: { id: 2, name: 'DRIVER' },
    SUPERVISOR: { id: 3, name: 'SUPERVISOR' },
    ADMIN: { id: 4, name: 'ADMIN' }
  };

  const defaultPermissions = {
    CLIENT: ['read:own_orders', 'create:orders'],
    DRIVER: ['read:orders', 'update:delivery_status', 'read:routes'],
    SUPERVISOR: ['read:reports', 'read:orders', 'update:orders', 'assign:drivers'],
    ADMIN: ['read:all', 'write:all', 'delete:all', 'manage:users']
  };

  const role = roleMap[name] || { id: 5, name };
  const perms = permissions.length > 0 
    ? permissions 
    : (defaultPermissions[name] || []);

  return {
    ...role,
    Permissions: perms.map((slug, index) => ({
      id: index + 1,
      slug,
      description: `Permission to ${slug}`
    }))
  };
}

// Helper para crear request mock
export function createMockRequest(overrides = {}) {
  return {
    body: overrides.body || {},
    params: overrides.params || {},
    query: overrides.query || {},
    headers: overrides.headers || {},
    cookies: overrides.cookies || {},
    user: overrides.user || null,
    ...overrides
  };
}

// Helper para crear response mock
export function createMockResponse() {
  const res = {
    status: function(code) { this.statusCode = code; return this; },
    json: function(data) { this.body = data; return this; },
    send: function(data) { this.body = data; return this; },
    cookie: function() { return this; },
    clearCookie: function() { return this; },
    setHeader: function() { return this; },
    headers: {}
  };
  return res;
}

// Helper para crear next mock
export function createMockNext() {
  return function next(err) { if (err) throw err; };
}
