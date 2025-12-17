# Sistema de Autenticación y Autorización - FleetService

## Descripción

Este servicio implementa un sistema de autenticación JWT con algoritmo ES256 (ECDSA) y autorización basada en scopes (permisos).

## Componentes

### 1. JwtMiddleware
**Ubicación:** `Middleware/JwtMiddleware.cs`

Middleware que intercepta todas las peticiones HTTP y:
- Extrae el token JWT del header `Authorization`
- Valida el token usando la llave pública EC (Elliptic Curve)
- Adjunta el usuario autenticado al contexto HTTP

### 2. ScopeRequirement
**Ubicación:** `Authorization/ScopeRequirement.cs`

Define un requisito de autorización basado en scopes (permisos). Permite especificar uno o más scopes requeridos para acceder a un recurso.

### 3. ScopeAuthorizationHandler
**Ubicación:** `Authorization/ScopeAuthorizationHandler.cs`

Handler que evalúa si un usuario cumple con los requisitos de scopes:
- Lee el claim `scope` del token JWT
- Verifica si el usuario tiene al menos uno de los scopes requeridos
- Devuelve 403 Forbidden si no tiene los permisos necesarios

## Configuración

### appsettings.Development.json
```json
{
  "Jwt": {
    "PublicKeyPath": "../src/keys/ec_public.pem"
  }
}
```

La ruta debe apuntar a la llave pública EC en formato PEM.

### Program.cs

Se configuran las siguientes políticas de autorización:

- **FleetView**: Requiere `fleet:view` - Ver información de la flota
- **FleetCreate**: Requiere `fleet:create` - Crear elementos de la flota
- **FleetUpdate**: Requiere `fleet:update` - Actualizar elementos de la flota
- **FleetManage**: Requiere `fleet:create` O `fleet:update` - Gestión completa

## Permisos de Fleet

Según el servicio de autenticación, los permisos de fleet son:

- `fleet:view` - Ver vehículos y conductores
- `fleet:create` - Crear vehículos y registrar conductores
- `fleet:update` - Actualizar estado de conductores y vehículos

## Estructura del Token JWT

El token JWT generado por el servicio de autenticación contiene:

```json
{
  "sub": "username",
  "user_id": "uuid",
  "role": "ADMIN|DRIVER|CLIENT|SUPERVISOR",
  "scope": "fleet:view fleet:create fleet:update",
  "zone_id": "uuid",
  "fleet_type": "LIGHT_VEHICLE|MOTORCYCLE|TRUCK"
}
```

## Uso en Controladores

### Nivel de Controlador
```csharp
[ApiController]
[Route("api/[controller]")]
[Authorize]  // Requiere autenticación para todos los endpoints
public class DriverController : ControllerBase
{
    // ...
}
```

### Nivel de Acción
```csharp
[HttpGet]
[Authorize(Policy = "FleetView")]
public async Task<IActionResult> GetAll()
{
    // Solo usuarios con el scope fleet:view pueden acceder
}

[HttpPost]
[Authorize(Policy = "FleetCreate")]
public async Task<IActionResult> Create([FromBody] DriverRequestDto dto)
{
    // Solo usuarios con el scope fleet:create pueden acceder
}

[HttpPatch("{id}/status")]
[Authorize(Policy = "FleetUpdate")]
public async Task<IActionResult> UpdateStatus(Guid id, [FromBody] UpdateDto dto)
{
    // Solo usuarios con el scope fleet:update pueden acceder
}
```

## Respuestas HTTP

- **401 Unauthorized**: Token inválido, expirado o no proporcionado
- **403 Forbidden**: Token válido pero sin los permisos necesarios
- **200 OK**: Autenticado y autorizado correctamente

## Flujo de Autenticación

1. Cliente obtiene token JWT del servicio de autenticación
2. Cliente envía petición con header: `Authorization: Bearer <token>`
3. `JwtMiddleware` valida el token con la llave pública
4. Token válido → Adjunta usuario al contexto HTTP
5. `ScopeAuthorizationHandler` verifica permisos
6. Tiene permisos → Acceso permitido
7. No tiene permisos → 403 Forbidden

## Ejemplo de Petición

```bash
curl -H "Authorization: Bearer eyJhbGc..." \
     http://localhost:8080/api/driver
```

## Notas de Seguridad

- El middleware valida la firma del token usando criptografía de curva elíptica (ES256)
- Los tokens expiran según la configuración del servicio de autenticación (por defecto 1h)
- No se requiere validar issuer ni audience ya que es un sistema interno
- La validación de lifetime está habilitada con ClockSkew = 0
