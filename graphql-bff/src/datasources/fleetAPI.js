// ============================================================
//  Data Source – FleetService  (.NET, puerto 5000→8080 interno)
//  Endpoints consumidos:
//    GET  /api/Driver           → todos los repartidores
//    GET  /api/Driver/:id       → repartidor por ID
//    GET  /api/Vehicle          → todos los vehículos
//    GET  /api/Vehicle/:id      → vehículo por ID
// ============================================================

const axios = require('axios');

class FleetAPI {
    constructor() {
        this.baseURL = process.env.FLEET_SERVICE_URL || 'http://localhost:5000';
        this.client = axios.create({
            baseURL: this.baseURL,
            timeout: Number(process.env.HTTP_TIMEOUT) || 5000,
            headers: { 'Content-Type': 'application/json' },
        });
    }

    /**
     * Inyectar el token JWT del request original para forwarding.
     * @param {string|null} token
     */
    setAuthToken(token) {
        if (token) {
            this.client.defaults.headers.common['Authorization'] = token;
        }
    }

    // ─── DRIVERS (Repartidores) ────────────────────────────

    /** GET /api/Driver → Lista de repartidores */
    async getAllDrivers(status, licenseCategory) {
        try {
            const params = {};
            if (status) params.status = status;
            if (licenseCategory) params.licenseCategory = licenseCategory;

            const { data } = await this.client.get('/api/Driver', { params });
            return data;
        } catch (err) {
            this._handleError(err, 'getAllDrivers');
            return [];
        }
    }

    /** GET /api/Driver/:id → Repartidor individual */
    async getDriverById(id) {
        try {
            const { data } = await this.client.get(`/api/Driver/${id}`);
            return data;
        } catch (err) {
            this._handleError(err, `getDriverById(${id})`);
            return null;
        }
    }

    /**
     * Batch: obtener múltiples repartidores por IDs (para DataLoader).
     * FleetService no tiene endpoint batch, paralelizamos llamadas individuales.
     * @param {string[]} ids
     * @returns {Promise<Array>} — misma posición que los ids de entrada
     */
    async getDriversByIds(ids) {
        const results = await Promise.allSettled(
            ids.map(id => this.getDriverById(id))
        );
        return results.map(r => (r.status === 'fulfilled' ? r.value : null));
    }

    // ─── VEHICLES (Vehículos) ──────────────────────────────

    /** GET /api/Vehicle → Lista de vehículos */
    async getAllVehicles() {
        try {
            const { data } = await this.client.get('/api/Vehicle');
            return data;
        } catch (err) {
            this._handleError(err, 'getAllVehicles');
            return [];
        }
    }

    /** GET /api/Vehicle/:id → Vehículo individual */
    async getVehicleById(id) {
        try {
            const { data } = await this.client.get(`/api/Vehicle/${id}`);
            return data;
        } catch (err) {
            this._handleError(err, `getVehicleById(${id})`);
            return null;
        }
    }

    /**
     * Batch: obtener múltiples vehículos por IDs (para DataLoader).
     * @param {string[]} ids
     * @returns {Promise<Array>}
     */
    async getVehiclesByIds(ids) {
        const results = await Promise.allSettled(
            ids.map(id => this.getVehicleById(id))
        );
        return results.map(r => (r.status === 'fulfilled' ? r.value : null));
    }

    /** Manejo centralizado de errores */
    _handleError(err, context) {
        if (err.response) {
            console.error(
                `[FleetAPI.${context}] HTTP ${err.response.status}: ${JSON.stringify(err.response.data)}`
            );
        } else if (err.request) {
            console.error(`[FleetAPI.${context}] Sin respuesta del servidor (FleetService posiblemente caído)`);
        } else {
            console.error(`[FleetAPI.${context}] Error: ${err.message}`);
        }
    }
}

module.exports = FleetAPI;
