import amqp from 'amqplib';

const RABBITMQ_CUSTOMER_EXCHANGE = 'customers-validation.exchange';
const RABBITMQ_CUSTOMER_ROUTING_KEY = 'customers.validation.routing';

const RABBITMQ_DRIVER_EXCHANGE = 'driver-user-created.exchange';
const RABBITMQ_DRIVER_ROUTING_KEY = 'driver.user.created.routing';

let connection = null;
let channel = null;

async function connectRabbitMQ() {
    try {
        if (connection && channel) {
            console.log(">>> [PRODUCER] Reutilizando conexión a RabbitMQ");
            return channel;
        }

        const host = process.env.RABBITMQ_HOST || 'localhost';
        const port = process.env.RABBITMQ_SVC_PORT || 5672;
        const user = process.env.RABBITMQ_USER || 'guest';
        const pass = process.env.RABBITMQ_PASSWORD || 'guest';

        const url = `amqp://${user}:${pass}@${host}:${port}`;

        console.log(`>>> [PRODUCER] Conectando a RabbitMQ en: ${host}:${port}`);
        connection = await amqp.connect(url);
        channel = await connection.createChannel();

        // Declarar exchanges que auth-ms usa para publicar
        await channel.assertExchange(RABBITMQ_CUSTOMER_EXCHANGE, 'topic', { durable: true });
        await channel.assertExchange(RABBITMQ_DRIVER_EXCHANGE, 'topic', { durable: true });

        console.log(`>>> [PRODUCER] Canal abierto. Exchanges declarados. Listo para publicar eventos`);

        return channel;
    } catch (error) {
        console.error('✗ [PRODUCER] Error conectando a RabbitMQ:', error.message);
        throw error;
    }
}

export async function publishUserCreatedEvent(user, role) {
    try {
        const channel = await connectRabbitMQ();

        const roleName = role?.name || 'UNKNOWN';

        // Solo publicar si el rol es CLIENT
        if (roleName !== 'CLIENT') {
            console.log(`ℹ [PRODUCER] Usuario creado con rol "${roleName}", no se publica evento de cliente`);
            return;
        }

        const event = {
            user_id: user.id,
            email: user.email,
            username: user.username,
            role_id: user.role_id,
            vehicle_type: user.vehicle_type || null,
            zone_id: user.zone_id || null,
            action: 'user_created',
            message: 'Nuevo usuario cliente creado',
            timestamp: Date.now()
        };

        const messageBuffer = Buffer.from(JSON.stringify(event));

        // Publicar en el exchange de cliente
        const published = channel.publish(
            RABBITMQ_CUSTOMER_EXCHANGE,
            RABBITMQ_CUSTOMER_ROUTING_KEY,
            messageBuffer,
            { persistent: true }
        );

        if (published) {
            console.log(`✓ [PRODUCER] Evento de CLIENT publicado: userId=${user.id}, email=${user.email}`);
        } else {
            console.warn(`⚠️ [PRODUCER] Buffer estaba lleno para evento de cliente`);
        }
    } catch (error) {
        console.error('✗ [PRODUCER] Error publicando evento de cliente:', error.message);
    }
}

/**
 * Publica un evento cuando se crea un usuario con rol DRIVER.
 * Esto notifica a FleetService para que cree el driver en fleet DB.
 * Si FleetService falla, publicará un rollback a auth-ms.
 */
export async function publishDriverCreatedEvent(user, role) {
    try {
        const channel = await connectRabbitMQ();

        const roleName = role?.name || 'UNKNOWN';

        // Solo publicar si el rol es DRIVER
        if (roleName !== 'DRIVER') {
            console.log(`ℹ [PRODUCER] Usuario creado con rol "${roleName}", no se publica evento de driver`);
            return;
        }

        const event = {
            user_id: user.id,
            email: user.email,
            username: user.username,
            role_id: user.role_id,
            vehicle_type: user.vehicle_type || null,
            zone_id: user.zone_id || null,
            action: 'driver_user_created',
            message: 'Nuevo usuario con rol DRIVER creado',
            timestamp: Date.now()
        };

        const messageBuffer = Buffer.from(JSON.stringify(event));

        // Publicar en el exchange de drivers
        const published = channel.publish(
            RABBITMQ_DRIVER_EXCHANGE,
            RABBITMQ_DRIVER_ROUTING_KEY,
            messageBuffer,
            { persistent: true }
        );

        if (published) {
            console.log(`✓ [PRODUCER] Evento de DRIVER publicado: userId=${user.id}, email=${user.email}`);
        } else {
            console.warn(`⚠️ [PRODUCER] Buffer estaba lleno para evento de driver`);
        }
    } catch (error) {
        console.error('✗ [PRODUCER] Error publicando evento de driver:', error.message);
    }
}

export async function closeRabbitMQ() {
    try {
        if (channel) {
            await channel.close();
            console.log("✓ [PRODUCER] Canal de RabbitMQ cerrado");
        }
        if (connection) {
            await connection.close();
            console.log("✓ [PRODUCER] Conexión de RabbitMQ cerrada");
        }
        channel = null;
        connection = null;
    } catch (error) {
        console.error('Error cerrando RabbitMQ:', error);
    }
}

