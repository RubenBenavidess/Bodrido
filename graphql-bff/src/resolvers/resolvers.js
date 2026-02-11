// ============================================================
//  Resolvers — Conectan el Schema con los Data Sources
//
//  Estrategia de resolución:
//    - Queries raíz: llaman directamente a los Data Sources
//    - Campos relacionales (Pedido.repartidor, Pedido.vehiculo,
//      Repartidor.vehiculo): usan DataLoaders para evitar N+1
//
//  Contexto esperado (inyectado por Apollo Server):
//    ctx.dataSources.orderAPI   → instancia de OrderAPI
//    ctx.dataSources.fleetAPI   → instancia de FleetAPI
//    ctx.loaders.driverLoader   → DataLoader para repartidores
//    ctx.loaders.vehicleLoader  → DataLoader para vehículos
// ============================================================

const { GraphQLScalarType, Kind } = require('graphql');

// ─── SCALAR JSON ─────────────────────────────────────────────
// Para el campo "features" de VehicleResponseDto (Dictionary<string, object>)
const JSONScalar = new GraphQLScalarType({
    name: 'JSON',
    description: 'Scalar type para JSON arbitrario',
    serialize(value) {
        return value;
    },
    parseValue(value) {
        return value;
    },
    parseLiteral(ast) {
        switch (ast.kind) {
            case Kind.STRING:
                return JSON.parse(ast.value);
            case Kind.OBJECT:
                return ast.fields.reduce((acc, field) => {
                    acc[field.name.value] = this.parseLiteral(field.value);
                    return acc;
                }, {});
            default:
                return null;
        }
    },
});

const resolvers = {
    // ─── SCALAR ──────────────────────────────────────────────
    JSON: JSONScalar,

    // ─── QUERIES ─────────────────────────────────────────────
    Query: {
        // ── Pedidos ──────────────────────────────────────────
        pedidos: async (_parent, _args, ctx) => {
            return ctx.dataSources.orderAPI.getAllOrders();
        },

        pedido: async (_parent, { id }, ctx) => {
            return ctx.dataSources.orderAPI.getOrderById(id);
        },

        pedidosPorCliente: async (_parent, { customerId }, ctx) => {
            return ctx.dataSources.orderAPI.getOrdersByCustomer(customerId);
        },

        // ── Repartidores ────────────────────────────────────
        repartidores: async (_parent, { status, licenseCategory }, ctx) => {
            return ctx.dataSources.fleetAPI.getAllDrivers(status, licenseCategory);
        },

        repartidor: async (_parent, { id }, ctx) => {
            return ctx.dataSources.fleetAPI.getDriverById(id);
        },

        // ── Vehículos ───────────────────────────────────────
        vehiculos: async (_parent, _args, ctx) => {
            return ctx.dataSources.fleetAPI.getAllVehicles();
        },

        vehiculo: async (_parent, { id }, ctx) => {
            return ctx.dataSources.fleetAPI.getVehicleById(id);
        },
    },

    // ─── PEDIDO: campos relacionales ─────────────────────────
    // Estos resolvers se ejecutan por cada Pedido en la lista.
    // Gracias a DataLoader, los IDs se agrupan y se resuelven
    // en UNA sola llamada batch al microservicio.
    Pedido: {
        /**
         * Resuelve el repartidor asignado al pedido.
         * Si driverId es null (pedido sin asignar), retorna null.
         * Si hay driverId, usa el DataLoader para obtenerlo.
         */
        repartidor: async (pedido, _args, ctx) => {
            if (!pedido.driverId) return null;
            return ctx.loaders.driverLoader.load(pedido.driverId);
        },

        /**
         * Resuelve el vehículo asignado al pedido.
         * vehicleId viene como String desde order-ms.
         */
        vehiculo: async (pedido, _args, ctx) => {
            if (!pedido.vehicleId) return null;
            return ctx.loaders.vehicleLoader.load(pedido.vehicleId);
        },
    },

    // ─── REPARTIDOR: campos relacionales ─────────────────────
    Repartidor: {
        /**
         * Resuelve el vehículo actualmente asignado al repartidor.
         * Usa el mismo vehicleLoader, aprovechando su cache:
         * si el vehículo ya fue cargado por un Pedido, no se
         * vuelve a pedir.
         */
        vehiculo: async (repartidor, _args, ctx) => {
            if (!repartidor.currentVehicleId) return null;
            return ctx.loaders.vehicleLoader.load(repartidor.currentVehicleId);
        },
    },
};

module.exports = resolvers;
