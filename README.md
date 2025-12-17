# Logiflow - Sistema de Logística Distribuido

Sistema de microservicios para gestión de logística con API Gateway Kong.

## 🏗️ Arquitectura

```
┌─────────────────────────────────────────────────────────────┐
│                    KONG API GATEWAY                         │
│                      Puerto: 8000                           │
└────────────────────┬────────────────────────────────────────┘
                     │
        ┌────────────┴────────────┐
        │                         │
┌───────▼──────────┐    ┌────────▼────────────┐
│  Auth Service    │    │  Fleet Service      │
│  (Node.js)       │    │  (.NET)             │
│  Puerto: 4000    │    │  Puerto: 8080       │
└────────┬─────────┘    └─────────┬───────────┘
         │                        │
    ┌────▼─────┐          ┌───────▼────┐
    │PostgreSQL│          │ PostgreSQL │
    │  :5432   │          │   :5433    │
    └──────────┘          └────────────┘
```

## 📋 Servicios

### 1. **Auth Service** (Node.js + Express)
- **Puerto**: 4000
- **Base de Datos**: PostgreSQL (puerto 5432)
- **Funcionalidad**: Autenticación y autorización con JWT (ES256)
- **Documentación**: `http://localhost:4000/api-docs`

### 2. **Fleet Service** (.NET + ASP.NET Core)
- **Puerto**: 8080 (expuesto como 5000)
- **Base de Datos**: PostgreSQL (puerto 5433)
- **Funcionalidad**: Gestión de flotas de vehículos

### 3. **Order Service** (Java Spring Boot)
- **Puerto**: 8080
- **Base de Datos**: PostgreSQL (puerto 5434)
- **Funcionalidad**: Gestión de órdenes y pedidos
- **Documentación**: `http://localhost:8080/swagger-ui.html`

### 4. **Kong API Gateway**
- **Puerto Gateway**: 8000 (clientes externos)
- **Puerto Admin**: 8001 (configuración)
- **Modo**: Declarativo (sin base de datos)

## 🚀 Inicio Rápido

### Requisitos Previos
- Docker >= 20.10
- Docker Compose >= 2.0
- Claves JWT en `src/keys/` (private.pem y public.pem)

### 1. Levantar todos los servicios

```bash
docker-compose up -d
```

### 2. Verificar que los servicios estén corriendo

```bash
docker-compose ps
```

### 3. Probar los endpoints a través de Kong

**Registro de usuario:**
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

**Login:**
```bash
curl -X POST http://localhost:8000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "SecurePass123!"
  }'
```

**Fleet Service:**
```bash
curl -X GET http://localhost:8000/api/fleet/vehicles \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Order Service:**
```bash
curl -X GET http://localhost:8000/api/orders \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## 🔧 Comandos Útiles

### Ver logs de un servicio específico
```bash
docker-compose logs -f auth-service
docker-compose logs -f fleet-service
docker-compose logs -f kong
```

### Reiniciar un servicio
```bash
docker-compose restart auth-service
```

### Detener todos los servicios
```bash
docker-compose down
```

### Detener y eliminar volúmenes (limpieza completa)
```bash
docker-compose down -v
```

### Reconstruir imágenes
```bash
docker-compose build
docker-compose up -d
```

## 📊 Verificar Estado de Kong

### Listar servicios configurados
```bash
curl http://localhost:8001/services
```

### Listar rutas configuradas
```bash
curl http://localhost:8001/routes
```

### Verificar plugins activos
```bash
curl http://localhost:8001/plugins
```

## 🔑 Generación de Claves JWT

Si no tienes las claves en `src/keys/`, generarlas:

```bash
# Crear directorio
mkdir -p src/keys

# Generar clave privada ES256
openssl ecparam -genkey -name prime256v1 -noout -out src/keys/private.pem

# Generar clave pública desde la privada
openssl ec -in src/keys/private.pem -pubout -out src/keys/public.pem
```

## 🌐 URLs de Acceso

| Servicio | URL Directa | URL a través de Kong |
|----------|-------------|---------------------|
| Auth Service | http://localhost:4000 | http://localhost:8000/api/auth |
| Fleet Service | http://localhost:5000 | http://localhost:8000/api/fleet |
| Order Service | http://localhost:8080 | http://localhost:8000/api/orders |
| Auth Swagger | http://localhost:4000/api-docs | - |
| Order Swagger | http://localhost:8080/swagger-ui.html | - |
| Kong Admin | http://localhost:8001 | - |

## 🗂️ Estructura del Proyecto

```
Bodrido/
├── docker-compose.yaml          # Orquestación de todos los servicios
├── gateway/
│   ├── docker-compose.yaml      # Kong standalone (deprecado)
│   └── kong.yml                 # Configuración declarativa de Kong
├── src/                         # Auth Service (Node.js)
│   ├── Dockerfile
│   ├── docker-compose.yml       # Standalone (deprecado)
│   ├── package.json
│   ├── server.js
│   ├── keys/                    # Claves JWT (no en git)
│   ├── controllers/
│   ├── services/
│   ├── models/
│   └── routes/
└── FleetService/                # Fleet Service (.NET)
    ├── Dockerfile
    ├── docker-compose.yml       # Standalone (deprecado)
    └── FleetService.csproj
```

## 🔒 Seguridad

- **JWT con ES256**: Autenticación asimétrica
- **Rate Limiting**: 100 peticiones/minuto por servicio
- **CORS**: Configurado para `http://localhost:5173`
- **Helmet.js**: Headers de seguridad en Auth Service
- **Usuario no-root**: Contenedores corren con usuarios limitados

## 🐛 Troubleshooting

### Error: "no such file or directory, open './keys/private.pem'"
```bash
# Verifica que las claves existan
ls -la src/keys/

# Si no existen, generarlas (ver sección "Generación de Claves JWT")
```

### Error: "Kong: 404 Not Found"
```bash
# Verificar configuración de Kong
curl http://localhost:8001/services

# Recargar configuración
docker-compose restart kong
```

### Error: "Cannot connect to database"
```bash
# Verificar que las bases de datos estén healthy
docker-compose ps

# Ver logs de las bases de datos
docker-compose logs auth-db
docker-compose logs fleet-db
```

### Error: "Port already in use"
```bash
# Ver qué proceso usa el puerto
sudo lsof -i :4000
sudo lsof -i :5432

# Detener el proceso o cambiar el puerto en docker-compose.yaml
```

## 📝 Variables de Entorno

### Auth Service
```env
NODE_ENV=production
PORT=4000
DB_HOST=auth-db
DB_PORT=5432
DB_USER=postgres
DB_PASSWORD=admin123
DB_NAME=logiflow_auth
PRIVATE_KEY_PATH=./keys/private.pem
PUBLIC_KEY_PATH=./keys/public.pem
```

### Fleet Service
```env
ASPNETCORE_ENVIRONMENT=Production
ASPNETCORE_URLS=http://+:8080
ConnectionStrings__DefaultConnection=Host=fleet-db;Port=5432;...
```

## 🧪 Testing

### Auth Service
```bash
cd src
npm test
```

### Fleet Service
```bash
cd FleetService
dotnet test
```

## 📚 Documentación Adicional

- [Kong Gateway Docs](https://docs.konghq.com/)
- [Swagger Auth Service](http://localhost:4000/api-docs)
- [Express.js Documentation](https://expressjs.com/)
- [ASP.NET Core Documentation](https://docs.microsoft.com/aspnet/core/)

## 👥 Equipo

- Desarrollo y Mantenimiento: Bodrido Team
- Repositorio: [RubenBenavidess/Bodrido](https://github.com/RubenBenavidess/Bodrido)

---

**Nota**: Este README asume que estás usando el `docker-compose.yaml` principal en la raíz del proyecto. Los archivos `docker-compose.yml` individuales en `src/` y `FleetService/` están deprecados en favor de la orquestación centralizada.
