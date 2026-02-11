// ============================================================
//  GraphQL BFF — Entry Point
//  Apollo Server v5 con DataLoaders per-request + Auth JWT
// ============================================================

require('dotenv').config();

const { ApolloServer } = require('@apollo/server');
const { startStandaloneServer } = require('@apollo/server/standalone');

const typeDefs = require('./schema/typeDefs');
const resolvers = require('./resolvers/resolvers');
const OrderAPI = require('./datasources/orderAPI');
const FleetAPI = require('./datasources/fleetAPI');
const createLoaders = require('./loaders/createLoaders');
const { authenticateRequest } = require('./auth/authenticate');

async function bootstrap() {
    // ── Crear instancia de Apollo Server ──────────────────
    const server = new ApolloServer({
        typeDefs,
        resolvers,
        introspection: true, // Habilitar playground/introspection
        formatError: (formattedError, _error) => {
            // No loguear errores de autenticación como errores graves
            if (formattedError?.extensions?.code === 'UNAUTHENTICATED') {
                console.warn('[Auth]', formattedError.message);
            } else {
                console.error('[GraphQL Error]', formattedError);
            }
            return formattedError;
        },
    });

    // ── Iniciar servidor standalone con contexto per-request ─
    const { url } = await startStandaloneServer(server, {
        listen: { port: Number(process.env.PORT) || 4001 },
        context: async ({ req }) => {
            // ─── AUTENTICACIÓN JWT ────────────────────────────
            // Valida el token con la misma clave pública EC (ES256)
            // que usan order-ms, FleetService, etc.
            // Si el token es inválido/ausente, lanza UNAUTHENTICATED.
            const { user, token } = authenticateRequest(req);

            // ─── DATA SOURCES ────────────────────────────────
            // Crear data sources frescos por request
            const orderAPI = new OrderAPI();
            const fleetAPI = new FleetAPI();

            // Forwardear el token original a los microservicios downstream
            orderAPI.setAuthToken(token);
            fleetAPI.setAuthToken(token);

            // ─── DATA LOADERS ────────────────────────────────
            // Crear DataLoaders frescos por request (evita cache stale)
            const loaders = createLoaders(fleetAPI);

            return {
                dataSources: { orderAPI, fleetAPI },
                loaders,
                user,   // Payload JWT decodificado (sub, user_id, role, scope, etc.)
                token,  // Token original para forwarding
            };
        },
    });

    console.log(`
  ╔══════════════════════════════════════════════════╗
  ║                                                  ║
  ║   🚀 GraphQL BFF corriendo en ${url}       ║
  ║   🔒 Autenticación JWT (ES256) activa            ║
  ║                                                  ║
  ║   📊 Playground:  ${url}                    ║
  ║                                                  ║
  ║   📡 Microservicios conectados:                  ║
  ║      • order-ms:     ${process.env.ORDER_SERVICE_URL}   ║
  ║      • FleetService: ${process.env.FLEET_SERVICE_URL}   ║
  ║                                                  ║
  ╚══════════════════════════════════════════════════╝
  `);
}

bootstrap().catch((err) => {
    console.error('❌ Error fatal al iniciar el servidor GraphQL:', err);
    process.exit(1);
});
