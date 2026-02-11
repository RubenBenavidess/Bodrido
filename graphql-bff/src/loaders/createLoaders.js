// ============================================================
//  DataLoaders — Eliminan el problema N+1
//
//  Funcionamiento:
//  1. Cuando GraphQL resuelve una lista de Pedidos, cada pedido
//     intenta resolver su campo "repartidor" y "vehiculo".
//  2. Sin DataLoader, se haría 1 HTTP call POR pedido (N+1).
//  3. Con DataLoader, se acumulan todos los IDs del mismo tick
//     del event loop y se hace UNA sola llamada batch.
//  4. DataLoader cachea los resultados dentro del mismo request,
//     evitando llamadas duplicadas.
//
//  IMPORTANTE: se crea un nuevo set de loaders POR REQUEST
//  para evitar cache stale entre distintos usuarios.
// ============================================================

const DataLoader = require('dataloader');

/**
 * Fábrica de DataLoaders. Se invoca una vez por request GraphQL.
 *
 * @param {import('../datasources/fleetAPI')} fleetAPI
 * @returns {{ driverLoader: DataLoader, vehicleLoader: DataLoader }}
 */
function createLoaders(fleetAPI) {
    // ─── DRIVER LOADER ───────────────────────────────────────
    // Agrupa todos los driverId solicitados en un tick y los
    // resuelve con una sola llamada batch a FleetService.
    const driverLoader = new DataLoader(
        async (driverIds) => {
            console.log(`[DataLoader] Batch de repartidores: [${driverIds.join(', ')}]`);

            // Obtener todos los drivers en paralelo
            const drivers = await fleetAPI.getDriversByIds(driverIds);

            // DataLoader exige que el array de resultados tenga el mismo
            // tamaño y orden que el array de keys de entrada.
            // getDriversByIds ya garantiza esto.
            return drivers;
        },
        {
            // Usar cacheKeyFn para normalizar UUIDs (case-insensitive)
            cacheKeyFn: (key) => String(key).toLowerCase(),
            // No cachear errores: si falla un driver, puede reintentarse
            // en la misma request si se consulta de nuevo.
        }
    );

    // ─── VEHICLE LOADER ──────────────────────────────────────
    // Mismo patrón: agrupa vehicleIds y los resuelve en batch.
    const vehicleLoader = new DataLoader(
        async (vehicleIds) => {
            console.log(`[DataLoader] Batch de vehículos: [${vehicleIds.join(', ')}]`);

            const vehicles = await fleetAPI.getVehiclesByIds(vehicleIds);
            return vehicles;
        },
        {
            cacheKeyFn: (key) => String(key).toLowerCase(),
        }
    );

    return { driverLoader, vehicleLoader };
}

module.exports = createLoaders;
