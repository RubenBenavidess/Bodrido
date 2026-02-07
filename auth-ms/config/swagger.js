// src/config/swagger.js
import swaggerJSDoc from "swagger-jsdoc";

const swaggerOptions = {
    definition: {
        openapi: "3.0.0",
        info: {
            title: "Auth Service API",
            version: "1.0.0",
            description: "Microservicio de Autenticación para LogiFlow",
            contact: {
                name: "SotoV"
            }
        },
        servers: [
            {
                url: "http://localhost:4000",
                description: "Servidor Local"
            }
        ],
        components: {
            securitySchemes: {
                bearerAuth: {
                    type: "http",
                    scheme: "bearer",
                    bearerFormat: "JWT"
                }
            }
        }
    },
    // Aquí le decimos dónde buscar los comentarios para generar la doc
    apis: ["./routes/*.js"] 
};

export const swaggerSpec = swaggerJSDoc(swaggerOptions);