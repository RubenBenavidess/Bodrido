import amqp from 'amqplib';

const RABBITMQ_ROLLBACK_EXCHANGE = 'user-rollback.exchange';
const RABBITMQ_ROLLBACK_QUEUE = 'user.rollback';
const RABBITMQ_ROLLBACK_ROUTING_KEY = 'user.rollback.routing';

let connection = null;
let channel = null;

async function connectRabbitMQ() {
    try {
        if (connection && channel) {
            console.log(">>> [LISTENER] Reutilizando conexión a RabbitMQ");
            return channel;
        }

        const host = process.env.RABBITMQ_HOST || 'localhost';
        const port = process.env.RABBITMQ_SVC_PORT || 5672; // Use SVC_PORT to avoid tcp:// string
        const user = process.env.RABBITMQ_USER || 'guest';
        const pass = process.env.RABBITMQ_PASSWORD || 'guest';

        // Ensure port is an integer
        const cleanPort = typeof port === 'string' ? parseInt(port.replace(/[^0-9]/g, '')) : port;

        const url = `amqp://${user}:${pass}@${host}:${cleanPort}`;

        console.log(`>>> [LISTENER] Conectando a RabbitMQ en: ${host}:${cleanPort}`);
        connection = await amqp.connect(url);
        channel = await connection.createChannel();

        // Declarar exchange de rollback (FleetService también lo declara)
        console.log(`>>> [LISTENER] Declarando exchange ${RABBITMQ_ROLLBACK_EXCHANGE}...`);
        await channel.assertExchange(RABBITMQ_ROLLBACK_EXCHANGE, 'topic', { durable: true });

        // Crear la cola de consumo
        console.log(`>>> [LISTENER] Creando cola ${RABBITMQ_ROLLBACK_QUEUE}...`);
        await channel.assertQueue(RABBITMQ_ROLLBACK_QUEUE, { durable: true });

        // Bindear queue a exchange (FleetService publica aquí cuando falla la creación del driver)
        await channel.bindQueue(RABBITMQ_ROLLBACK_QUEUE, RABBITMQ_ROLLBACK_EXCHANGE, RABBITMQ_ROLLBACK_ROUTING_KEY);
        console.log(`✓ [LISTENER] Cola ${RABBITMQ_ROLLBACK_QUEUE} bindeada a ${RABBITMQ_ROLLBACK_EXCHANGE}`);

        return channel;
    } catch (error) {
        console.error('✗ [LISTENER] Error conectando a RabbitMQ:', error.message);
        throw error;
    }
}

export async function setupUserRollbackListener(messageHandler) {
    try {
        const channel = await connectRabbitMQ();

        console.log(`>>> [LISTENER] Consumiendo mensajes de ${RABBITMQ_ROLLBACK_QUEUE}...`);
        await channel.consume(RABBITMQ_ROLLBACK_QUEUE, async (msg) => {
            if (msg) {
                try {
                    const event = JSON.parse(msg.content.toString());
                    console.log(`📨 [LISTENER] Evento de compensación recibido: userId=${event.user_id}, reason=${event.reason}`);

                    // Llamar al handler proporcionado
                    await messageHandler(event);

                    // Reconocer el mensaje para que no se reintente
                    channel.ack(msg);
                    console.log(`✓ [LISTENER] Evento procesado y reconocido`);
                } catch (error) {
                    console.error('✗ [LISTENER] Error procesando evento:', error.message);
                    // Rechazar sin reencolar
                    channel.nack(msg, false, false);
                }
            }
        }, { noAck: false });

        console.log(`✓ [LISTENER] Listener de compensación configurado correctamente`);
    } catch (error) {
        console.error('✗ [LISTENER] Error configurando listener:', error.message);
        throw error;
    }
}

export async function closeRabbitMQ() {
    try {
        if (channel) {
            await channel.close();
            console.log("✓ [LISTENER] Canal cerrado");
        }
        if (connection) {
            await connection.close();
            console.log("✓ [LISTENER] Conexión cerrada");
        }
        channel = null;
        connection = null;
    } catch (error) {
        console.error('✗ [LISTENER] Error cerrando conexión:', error.message);
    }
}

