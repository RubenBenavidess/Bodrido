# Guía Rápida de Inicio - Logiflow

## 🚀 Inicio en 3 pasos

### 1. Verifica la configuración
```bash
./verify.sh
```

### 2. Genera las claves JWT (si no existen)
```bash
make keys
```

### 3. Inicia todos los servicios
```bash
make start
# o
./start.sh
```

## 📋 Comandos útiles con Makefile

```bash
make help          # Ver todos los comandos disponibles
make start         # Iniciar servicios
make stop          # Detener servicios
make restart       # Reiniciar servicios
make logs          # Ver logs de todos los servicios
make logs-auth     # Ver logs del Auth Service
make logs-fleet    # Ver logs del Fleet Service
make logs-kong     # Ver logs de Kong
make status        # Ver estado de los servicios
make test          # Ejecutar tests
make check-kong    # Verificar configuración de Kong
make test-endpoints # Probar endpoints
make clean         # Limpiar contenedores y volúmenes
```

## 🔌 Puertos y URLs

| Servicio | Puerto | URL |
|----------|--------|-----|
| Kong Gateway | 8000 | http://localhost:8000 |
| Kong Admin | 8001 | http://localhost:8001 |
| Auth Service | 4000 | http://localhost:4000 |
| Auth Swagger | 4000 | http://localhost:4000/api-docs |
| Fleet Service | 5000 | http://localhost:5000 |
| Auth DB | 5432 | localhost:5432 |
| Fleet DB | 5433 | localhost:5433 |

## 🧪 Probar los endpoints

### Registro de usuario
```bash
curl -X POST http://localhost:8000/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "SecurePass123!",
    "role": "CLIENT"
  }'
```

### Login
```bash
curl -X POST http://localhost:8000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "SecurePass123!"
  }'
```

### Verificar token
```bash
curl -X GET http://localhost:8000/api/auth/verify \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## 🐛 Troubleshooting

### Ver logs de un servicio
```bash
docker-compose logs -f auth-service
docker-compose logs -f fleet-service
docker-compose logs -f kong
```

### Reiniciar un servicio específico
```bash
docker-compose restart auth-service
```

### Limpiar todo y empezar de cero
```bash
make clean
make start
```

### Acceder a la base de datos
```bash
make db-auth   # Auth Service database
make db-fleet  # Fleet Service database
```

### Acceder al shell de un contenedor
```bash
make shell-auth   # Auth Service
make shell-fleet  # Fleet Service
make shell-kong   # Kong Gateway
```

## 📚 Más información

Ver `README.md` para documentación completa.
