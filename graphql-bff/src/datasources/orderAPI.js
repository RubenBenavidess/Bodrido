// ============================================================
//  Data Source – order-ms  (Java Spring Boot, puerto 8080)
//  Endpoints consumidos:
//    GET  /orders           → todos los pedidos
//    GET  /orders/:id       → pedido por ID
//    GET  /orders/customer/:customerId → pedidos por cliente
// ============================================================

const axios = require('axios');

class OrderAPI {
    constructor() {
        this.baseURL = process.env.ORDER_SERVICE_URL || 'http://localhost:8080';
        this.client = axios.create({
            baseURL: this.baseURL,
            timeout: Number(process.env.HTTP_TIMEOUT) || 5000,
            headers: { 'Content-Type': 'application/json' },
        });
    }

    /**
     * Inyectar el token JWT del request original para forwarding.
     * @param {string|null} token - Bearer token
     */
    setAuthToken(token) {
        if (token) {
            this.client.defaults.headers.common['Authorization'] = token;
        }
    }

    /** GET /orders → Lista de pedidos */
    async getAllOrders() {
        try {
            const { data } = await this.client.get('/orders');
            return data;
        } catch (err) {
            this._handleError(err, 'getAllOrders');
            return [];
        }
    }

    /** GET /orders/:id → Pedido individual */
    async getOrderById(id) {
        try {
            const { data } = await this.client.get(`/orders/${id}`);
            return data;
        } catch (err) {
            this._handleError(err, `getOrderById(${id})`);
            return null;
        }
    }

    /** GET /orders/customer/:customerId → Pedidos de un cliente */
    async getOrdersByCustomer(customerId) {
        try {
            const { data } = await this.client.get(`/orders/customer/${customerId}`);
            return data;
        } catch (err) {
            // 204 No Content devuelve body vacío
            if (err.response && err.response.status === 204) return [];
            this._handleError(err, `getOrdersByCustomer(${customerId})`);
            return [];
        }
    }

    /**
     * Obtener múltiples pedidos por sus IDs (para DataLoader).
     * order-ms no tiene endpoint batch, así que paralelizamos las llamadas individuales.
     * @param {string[]} ids
     * @returns {Promise<Array>}
     */
    async getOrdersByIds(ids) {
        const results = await Promise.allSettled(
            ids.map(id => this.getOrderById(id))
        );
        // Mantener el orden y manejar fallos → null
        return results.map(r => (r.status === 'fulfilled' ? r.value : null));
    }

    /** Manejo centralizado de errores */
    _handleError(err, context) {
        if (err.response) {
            console.error(
                `[OrderAPI.${context}] HTTP ${err.response.status}: ${JSON.stringify(err.response.data)}`
            );
        } else if (err.request) {
            console.error(`[OrderAPI.${context}] Sin respuesta del servidor (order-ms posiblemente caído)`);
        } else {
            console.error(`[OrderAPI.${context}] Error: ${err.message}`);
        }
    }
}

module.exports = OrderAPI;
