# 🚀 GraphQL BFF — Bodrido / Logiflow

Servicio **Backend for Frontend** que expone una API GraphQL unificada,
consumiendo los microservicios REST existentes (`order-ms` y `FleetService`).

## Arquitectura

```
┌─────────────────────────────────────────────────────────────┐
│                         Cliente                             │
│               (Web App / Mobile / Postman)                  │
└─────────────────────┬───────────────────────────────────────┘
                      │  GraphQL Query
                      ▼
┌─────────────────────────────────────────────────────────────┐
│                   GraphQL BFF (:4001)                        │
│                                                             │
│  ┌──────────┐  ┌──────────────┐  ┌────────────────────┐    │
│  │  Schema   │  │  Resolvers   │  │   DataLoaders      │    │
│  │ (typeDefs)│  │              │  │ (N+1 prevention)   │    │
│  └──────────┘  └──────┬───────┘  └──────┬─────────────┘    │
│                       │                 │                   │
│               ┌───────┴─────┐   ┌───────┴─────┐            │
│               │  OrderAPI   │   │  FleetAPI   │            │
│               │ (datasource)│   │ (datasource)│            │
│               └──────┬──────┘   └──────┬──────┘            │
└──────────────────────┼─────────────────┼────────────────────┘
                       │                 │
              HTTP/REST│        HTTP/REST│
                       ▼                 ▼
             ┌─────────────┐    ┌─────────────┐
             │  order-ms   │    │ FleetService│
             │ (Java:8080) │    │ (.NET:5000) │
             └─────────────┘    └─────────────┘
```

## Tipos GraphQL

| Tipo          | Fuente         | Relaciones                              |
|---------------|----------------|-----------------------------------------|
| `Pedido`      | order-ms       | → `Repartidor` (via driverId)           |
|               |                | → `Vehiculo` (via vehicleId)            |
| `Repartidor`  | FleetService   | → `Vehiculo` (via currentVehicleId)     |
| `Vehiculo`    | FleetService   | (tipo raíz)                             |

## DataLoaders — Prevención N+1

### Sin DataLoader (problema N+1):
```
Query pedidos → 1 call a order-ms (obtiene 50 pedidos)
  Para cada pedido:
    → 1 call a FleetService/Driver/:id    (50 calls!)
    → 1 call a FleetService/Vehicle/:id   (50 calls!)
Total: 1 + 50 + 50 = 101 HTTP requests 💀
```

### Con DataLoader (solución):
```
Query pedidos → 1 call a order-ms (obtiene 50 pedidos)
  DataLoader agrupa todos los driverIds → 1 batch paralelo
  DataLoader agrupa todos los vehicleIds → 1 batch paralelo
  + Cache per-request evita duplicados
Total: 1 + N_unique_drivers + N_unique_vehicles ≈ pocos calls ✅
```

## Ejecución Local

```bash
# Instalar dependencias
cd graphql-bff
npm install

# Configurar .env (ya tiene defaults)
# Asegúrate de que order-ms y FleetService estén corriendo

# Iniciar
npm start

# Desarrollo con --watch
npm run dev
```

El servidor arranca en `http://localhost:4001` con Apollo Sandbox habilitado.

## Con Docker Compose

```bash
docker compose up graphql-bff
```

O levantar todo:
```bash
docker compose up -d
```

Accesible via Kong Gateway en: `http://localhost:8000/api/graphql`

## Queries de Ejemplo

### Todos los pedidos con repartidor y vehículo
```graphql
query PedidosCompletos {
  pedidos {
    id
    status
    totalAmount
    distanceKm
    deliveryAddress {
      street
      city
      coordinates {
        latitude
        longitude
      }
    }
    pickupAddress {
      street
      city
    }
    orderItems {
      description
      quantity
      weightKg
    }
    repartidor {
      id
      licenseNumber
      licenseCategory
      status
      vehiculo {
        plate
        brand
        model
        type
      }
    }
    vehiculo {
      id
      plate
      brand
      model
      type
      condition
      maxLoadKg
    }
  }
}
```

### Pedido individual con todo el detalle
```graphql
query PedidoDetalle($id: ID!) {
  pedido(id: $id) {
    id
    customerId
    status
    totalAmount
    tripFee
    serviceFee
    distanceKm
    orderDate
    deliveryAddress {
      street
      city
      instructions
      coordinates {
        latitude
        longitude
      }
    }
    repartidor {
      id
      licenseNumber
      status
      vehiculo {
        plate
        brand
        model
        maxLoadKg
      }
    }
    vehiculo {
      plate
      type
      condition
    }
  }
}
```

### Repartidores disponibles con sus vehículos
```graphql
query RepartidoresDisponibles {
  repartidores(status: AVAILABLE) {
    id
    licenseNumber
    licenseCategory
    status
    vehiculo {
      id
      plate
      brand
      model
      type
      condition
      isAssigned
    }
  }
}
```

### Flota de vehículos
```graphql
query FlotaCompleta {
  vehiculos {
    id
    plate
    brand
    model
    type
    condition
    maxLoadKg
    volumeM3
    isAssigned
    currentZoneId
    features
  }
}
```

## Headers Requeridos

El servicio propaga el token JWT a los microservicios downstream:

```
Authorization: Bearer <tu-jwt-token>
```
