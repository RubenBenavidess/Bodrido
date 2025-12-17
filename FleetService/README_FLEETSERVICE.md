# Fleet Service
API para la gestión de conductores y vehículos.

## Configuración

### Requisitos
- Docker y Docker Compose

## Ejecución para testing con Docker Compose
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

## Endpoints a probar

### Driver Controller
- `GET /api/driver/` - Obtener listado conductores
- `POST /api/driver` - Registrar conductor
- `GET /api/driver/{id}` - Obtener conductor por ID
- `GET /api/driver/user/{userId}` - Obtener conductor por userId
- `PATCH /api/driver/{id}/status` - Actualizar estado del conductor

### Vehicle Controller
- `GET /api/vehicle` - Obtener todos los vehículos
- `GET /api/vehicle/{id}` - Obtener vehículo por ID
- `POST /api/vehicle` - Crear vehículo