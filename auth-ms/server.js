import dotenv from "dotenv";
dotenv.config();

import connect, { sequelize } from "./config/db.js"; // Importa sequelize también
import defineAssociations from "./models/associations.js"; // <--- IMPORTANTE

// import hashCache from "./logs/hashCache.js"; // (Comenta si no usas logs aun)

import express from "express";
import router from "./routes/authRouter.js";
import helmet from "helmet";
import cors from "cors";
import handleErrors from "./middleware/errors/errorMIddleware.js";
import cookieParser from "cookie-parser";
import swaggerUi from "swagger-ui-express"; 
import { swaggerSpec } from "./config/swagger.js";

// 1. Inicializar App
const app = express();

app.use("/api-docs", swaggerUi.serve, swaggerUi.setup(swaggerSpec));

app.get("/api-docs.json", (req, res) => {
    res.setHeader("Content-Type", "application/json");
    res.send(swaggerSpec);
});

// 2. Middlewares
app.use(helmet());
app.use(cookieParser());
app.use(express.json()); // Mueve esto arriba antes del router

const CORS_OPTIONS = {
    origin: 'http://localhost:5173',
    optionsSuccessStatus: 200
};
app.use(cors(CORS_OPTIONS));

// 3. Rutas
app.use(router);

// 4. Endpoint 404
app.use((req, res) => {
    res.status(404).json({
        success: false,
        message: `Not Found: ${req.method} ${req.originalUrl}`
    });
});

// 5. Error middleware
app.use(handleErrors);

// 6. Inicialización de BDD y Servidor
const PORT = process.env.PORT;

// Función autoejecutable para orden asíncrono
(async () => {
    try {
        // A. Conectar a BDD
        await connect(); 
        
        // B. Definir relaciones (CRÍTICO: Antes de sync)
        defineAssociations();
        
        // C. Sincronizar modelos (Crea tablas FK correctamente)
        // En prod usa { alter: true } con cuidado, o migraciones
        await sequelize.sync({ alter: true });
        console.log("Tablas sincronizadas con asociaciones");

        // D. Levantar servidor
        app.listen(PORT, () => {
            console.log(`Auth Microservice running on port ${PORT}!`);
        });

    } catch (error) {
        console.error("Fallo fatal al iniciar:", error);
    }
})();