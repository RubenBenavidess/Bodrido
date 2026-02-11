// ============================================================
//  GraphQL Schema – Tipos: Pedido, Repartidor, Vehiculo
//  Refleja los modelos reales de order-ms y FleetService
// ============================================================

const typeDefs = `#graphql

  # ─── ENUMS ────────────────────────────────────────────────

  "Estado del pedido (order-ms)"
  enum EstadoPedido {
    PENDING
    CREATED
    ASSIGNMENT_PENDING
    ASSIGNED
    PICKED_UP
    IN_ROUTE
    DELIVERED
    CANCELLED
  }

  "Estado del repartidor (FleetService)"
  enum EstadoRepartidor {
    INACTIVE
    OFF_DUTY
    AVAILABLE
    BUSY
    ON_BREAK
  }

  "Tipo de vehículo (FleetService)"
  enum TipoVehiculo {
    MOTORCYCLE
    LIGHT_VEHICLE
    TRUCK
  }

  "Condición del vehículo (FleetService)"
  enum CondicionVehiculo {
    OPERATIONAL
    MAINTENANCE
    DECOMMISSIONED
  }

  "Categoría de licencia del repartidor"
  enum CategoriaLicencia {
    A
    B
    C
    C1
    D
    D1
    E
    E1
    F
    G
  }

  # ─── TIPOS AUXILIARES ─────────────────────────────────────

  "Coordenadas geográficas"
  type Coordenadas {
    longitude: Float
    latitude: Float
  }

  "Dirección con calle, ciudad, coordenadas e instrucciones"
  type Direccion {
    street: String
    city: String
    coordinates: Coordenadas
    instructions: String
  }

  "Ubicación simple (lat/lng)"
  type Ubicacion {
    lat: Float
    lng: Float
  }

  # ─── ITEM DEL PEDIDO ─────────────────────────────────────

  "Artículo individual dentro de un pedido"
  type ItemPedido {
    id: ID!
    description: String!
    quantity: Int!
    weightKg: Float
    declaredValue: Float
    handlingFee: Float
  }

  # ─── TIPO PRINCIPAL: VEHICULO ─────────────────────────────

  """
  Vehículo de la flota.
  Puede ser MOTORCYCLE, LIGHT_VEHICLE o TRUCK.
  Mapeado desde FleetService (VehicleResponseDto).
  """
  type Vehiculo {
    id: ID!
    type: TipoVehiculo!
    plate: String!
    brand: String!
    model: String!
    maxLoadKg: Float!
    volumeM3: Float!
    currentZoneId: String
    condition: CondicionVehiculo!
    isAssigned: Boolean!
    features: JSON
  }

  # ─── TIPO PRINCIPAL: REPARTIDOR ───────────────────────────

  """
  Repartidor / Conductor de la flota.
  Mapeado desde FleetService (DriverResponseDto).
  Relación: un Repartidor puede tener un Vehiculo asignado.
  """
  type Repartidor {
    id: ID!
    userId: ID!
    licenseNumber: String!
    licenseCategory: CategoriaLicencia!
    status: EstadoRepartidor!
    currentVehicleId: ID
    currentVehiclePlate: String

    "Vehículo actualmente asignado (resuelto via DataLoader)"
    vehiculo: Vehiculo
  }

  # ─── TIPO PRINCIPAL: PEDIDO ───────────────────────────────

  """
  Pedido / Orden de envío.
  Mapeado desde order-ms (OrderResponse).
  Relaciones:
    - repartidor: el conductor asignado (via DataLoader → FleetService)
    - vehiculo: el vehículo asignado (via DataLoader → FleetService)
  """
  type Pedido {
    id: ID!
    customerId: ID!
    driverId: ID
    vehicleId: ID
    status: EstadoPedido!
    distanceKm: Float
    tripFee: Float
    serviceFee: Float
    totalAmount: Float
    deliveryAddress: Direccion
    pickupAddress: Direccion
    orderItems: [ItemPedido!]!
    orderDate: String

    "Repartidor asignado (resuelto via DataLoader, evita N+1)"
    repartidor: Repartidor

    "Vehículo asignado (resuelto via DataLoader, evita N+1)"
    vehiculo: Vehiculo
  }

  # ─── SCALAR PERSONALIZADO ────────────────────────────────

  "Scalar para campos JSON arbitrarios (features del vehículo)"
  scalar JSON

  # ─── QUERIES ──────────────────────────────────────────────

  type Query {
    "Obtener todos los pedidos"
    pedidos: [Pedido!]!

    "Obtener un pedido por su ID"
    pedido(id: ID!): Pedido

    "Obtener pedidos de un cliente específico"
    pedidosPorCliente(customerId: ID!): [Pedido!]!

    "Obtener todos los repartidores, con filtros opcionales"
    repartidores(status: EstadoRepartidor, licenseCategory: CategoriaLicencia): [Repartidor!]!

    "Obtener un repartidor por su ID"
    repartidor(id: ID!): Repartidor

    "Obtener todos los vehículos de la flota"
    vehiculos: [Vehiculo!]!

    "Obtener un vehículo por su ID"
    vehiculo(id: ID!): Vehiculo
  }
`;

module.exports = typeDefs;
