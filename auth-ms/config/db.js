import { Sequelize } from "sequelize";
import dotenv from "dotenv";

dotenv.config();

const sequelize = new Sequelize(
    process.env.DB_NAME,
    process.env.DB_USER,
    process.env.DB_PASSWORD,
    {
        host: process.env.DB_HOST,
        dialect: "postgres",
        logging: false,
    }
);

export default async function connect() {
    try {
        await sequelize.authenticate();
        console.log("Conexión exitosa a PostgreSQL (Sequelize)");
    } catch (error) {
        console.error("Error al conectar a PostgreSQL:", error);
        process.exit(1); // Si falla la base, mejor matar el proceso
    }
}

export { sequelize };