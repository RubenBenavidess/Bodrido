# Fleet Service

API para la gestión de conductores y vehículos.

## Configuración

### Requisitos
- .NET 10.0 SDK
- PostgreSQL 17
- Docker y Docker Compose (opcional)

## Ejecución Local

### 1. Base de datos PostgreSQL
Asegúrate de tener PostgreSQL corriendo en `localhost:5432` con las credenciales configuradas en `appsettings.json`.

### 2. Crear la migración (primera vez)
```bash
dotnet ef migrations add InitialCreate
```

### 3. Ejecutar la aplicación
```bash
dotnet run
```

La aplicación estará disponible en:
- Swagger UI: http://localhost:5000 (o https://localhost:5001)

## Ejecución con Docker Compose

### 1. Crear la migración
```bash
dotnet ef migrations add InitialCreate
```

### 2. Construir y ejecutar los contenedores
```bash
docker-compose up --build
```

La aplicación estará disponible en:
- API: http://localhost:8080
- Swagger UI: http://localhost:8080

### Detener los contenedores
```bash
docker-compose down
```

### Detener y eliminar volúmenes (limpiar datos)
```bash
docker-compose down -v
```

## Migraciones de Base de Datos

### Las migraciones se aplican automáticamente
El `Program.cs` está configurado para aplicar las migraciones automáticamente al iniciar la aplicación:
```csharp
context.Database.Migrate();
```

Esto significa que:
- **No necesitas** ejecutar `dotnet ef database update` manualmente
- Al arrancar la app, se crearán/actualizarán las tablas automáticamente
- Funciona tanto en desarrollo local como en contenedores

### Crear una nueva migración
```bash
dotnet ef migrations add NombreDeLaMigracion
```

### Revertir la última migración
```bash
dotnet ef migrations remove
```

## Endpoints Principales

### Driver Controller
- `POST /api/driver` - Registrar conductor
- `GET /api/driver/{id}` - Obtener conductor por ID
- `GET /api/driver/user/{userId}` - Obtener conductor por userId
- `PATCH /api/driver/{id}/status` - Actualizar estado del conductor

### Vehicle Controller
- `GET /api/vehicle` - Obtener todos los vehículos
- `GET /api/vehicle/{id}` - Obtener vehículo por ID
- `POST /api/vehicle` - Crear vehículo

## Notas para Producción

Para producción, cambia las credenciales de la base de datos usando variables de entorno:
```bash
export ConnectionStrings__DefaultConnection="Host=your-host;Port=5432;Database=FleetDB;Username=your-user;Password=your-password"
```
