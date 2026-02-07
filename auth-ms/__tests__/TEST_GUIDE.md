# Tests - Auth Microservice

Este directorio contiene las pruebas unitarias e integración para el microservicio de autenticación.

## 📋 Cobertura de Pruebas

### 1. **Pruebas Unitarias** (`authService.test.js`)
- ✅ Registro de usuarios con validación de datos
- ✅ Login con credenciales válidas/inválidas
- ✅ Validación de roles (ADMIN, DRIVER, CLIENT, SUPERVISOR)
- ✅ Generación de tokens JWT y Refresh tokens
- ✅ Manejo de errores y rollback de transacciones
- ✅ Validación de campos requeridos según rol

### 2. **Pruebas de Integración** (`authController.test.js`)
- ✅ POST `/auth/register` - Registro con códigos 201, 400, 409
- ✅ POST `/auth/login` - Login con códigos 200, 401, 400
- ✅ POST `/auth/token/refresh` - Renovación de tokens
- ✅ POST `/auth/logout` - Cierre de sesión
- ✅ GET `/auth/verify` - Verificación de tokens
- ✅ Validación de cookies y headers
- ✅ Manejo de errores con códigos HTTP apropiados

### 3. **Pruebas de Autenticación** (`authorization.test.js`)
#### 401 UNAUTHORIZED - No Autenticado
- ✅ Rechazo sin token de autenticación
- ✅ Rechazo con token inválido
- ✅ Rechazo con token expirado
- ✅ Rechazo con formato de token incorrecto

#### Validación de Claims del Token
- ✅ Validación de claims para CLIENT (role, scope)
- ✅ Validación de claims para DRIVER (role, scope, zone_id, fleet_type)
- ✅ Validación de claims para ADMIN (role, scope con permisos amplios)
- ✅ Validación de claims para SUPERVISOR (role, scope, zone_id)
- ✅ Validación de estructura del token (sub, user_id, username)
- ✅ Validación de formato de scope (space-separated)

## 🎯 Casos de Prueba del Microservicio de Autenticación

### ✅ Registro de Usuarios con Validación de Roles
```javascript
// authController.test.js - Validación de campos requeridos
it('should validate required fields for DRIVER role', ...)
// Valida que DRIVER tenga vehicle_type y zone_id
```

### ✅ Generación de Tokens JWT con Claims Correctos
```javascript
// authService.test.js - Login genera token con claims
it('should login successfully with valid credentials', ...)
// Genera accessToken con role, scope, zone_id, fleet_type
```

### ✅ Rechazo de Petición No Autenticada (401)
```javascript
// authorization.test.js
describe('401 UNAUTHORIZED - No Token or Invalid Token', () => {
  it('should reject protected endpoint without token (401)', ...)
  it('should reject with invalid token format (401)', ...)
  it('should reject with expired token (401)', ...)
})
```

### ✅ Validación de Estructura del Token
```javascript
// authorization.test.js
describe('Token Claims Validation', () => {
  it('should validate token contains required claims for CLIENT', ...)
  it('should validate token contains required claims for DRIVER', ...)
  it('should validate scope format (space-separated permissions)', ...)
})
```

### ⚠️ Fuera del Alcance (Pertenece a Otros Microservicios)
Los siguientes casos de prueba **NO** pertenecen al microservicio de autenticación:
- ❌ **403 FORBIDDEN** - Validación de permisos por endpoint (corresponde al Gateway o microservicio de pedidos)
- ❌ Creación/asignación de pedidos (corresponde al microservicio de pedidos)
- ❌ Middleware de autorización por rol/permiso (corresponde al Gateway)

## 🚀 Ejecutar Pruebas

```bash
# Ejecutar todas las pruebas
npm test

# Ejecutar con cobertura
npm test -- --coverage

# Ejecutar solo pruebas de autorización
npm test authorization.test.js

# Ejecutar en modo watch
npm run test:watch

# Ver reporte de cobertura en navegador
open coverage/lcov-report/index.html
```

## 📊 Estructura de Archivos

```
__tests__/
├── setup.js                    # Configuración global de mocks y helpers
├── authService.test.js         # Pruebas unitarias del servicio
├── authController.test.js      # Pruebas de integración del controlador
├── authorization.test.js       # Pruebas de autenticación (401/403)
└── TEST_GUIDE.md              # Este archivo
```

## 🔧 Configuración

### jest.config.js
- **testEnvironment**: `node`
- **coverage**: 60% mínimo en branches, functions, lines, statements
- **setupFiles**: `setup.js` con mocks globales y helpers

### Helpers Disponibles (setup.js)
- `createMockToken(payload)` - Crea token JWT mock
- `createMockUser(overrides)` - Crea usuario mock con roles
- `createMockRole(name, permissions)` - Crea rol con permisos
- `createMockRequest(overrides)` - Request mock para middleware
- `createMockResponse()` - Response mock con spies
- `createMockNext()` - Next function mock

## 📝 Ejemplos de Uso

### Probar Endpoint Protegido
```javascript
const response = await request(app)
  .get('/protected-route')
  .set('Authorization', 'Bearer valid-token')
  .expect(200);
```

### Probar Rechazo 401
```javascript
const response = await request(app)
  .get('/protected-route')
  .expect(401);

expect(response.body.error).toBe('UNAUTHORIZED');
```

### Probar Validación de Claims del Token
```javascript
jwtManager.validateToken.mockReturnValue({
  user_id: 'driver-123',
  username: 'driver_user',
  role: 'DRIVER',
  scope: 'read:orders update:delivery_status',
  zone_id: 3,
  fleet_type: 'LIGHT_VEHICLE'
});

const response = await request(app)
  .get('/token/claims')
  .set('Authorization', 'Bearer valid-token')
  .expect(200);

expect(response.body.claims.role).toBe('DRIVER');
expect(response.body.claims.zone_id).toBe(3);
expect(response.body.claims.fleet_type).toBe('LIGHT_VEHICLE');
```

## 🎓 Convenciones

- ✅ Usar `describe` para agrupar casos relacionados
- ✅ Nombres descriptivos: `should [acción] when [condición]`
- ✅ Limpiar mocks con `jest.clearAllMocks()` en `beforeEach`
- ✅ Usar códigos HTTP correctos (200, 201, 400, 401, 403, 409, 500)
- ✅ Validar estructura de respuesta (`success`, `error`, `message`)
- ✅ Probar casos happy path y edge cases

## 📚 Referencias

- [Jest Documentation](https://jestjs.io/)
- [Supertest](https://github.com/visionmedia/supertest)
- [HTTP Status Codes](https://developer.mozilla.org/en-US/docs/Web/HTTP/Status)
