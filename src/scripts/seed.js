// src/scripts/seed.js
import { sequelize } from "../config/db.js";
import Role from "../models/Role.js";
import Permission from "../models/Permission.js";
import Zone from "../models/Zone.js";
import defineAssociations from "../models/associations.js"; // Importa tus asociaciones

async function seed() {
    try {
        await sequelize.authenticate();
        defineAssociations(); // Importante definir relaciones antes de sincronizar
        await sequelize.sync({ force: true }); // OJO: force:true BORRA TODO y crea de cero. Solo para seed inicial.

        console.log("Iniciando Seed...");

        // 1. Crear Permisos (Scopes)
        const p1 = await Permission.create({ slug: "order:create", description: "Crear pedidos" });
        const p2 = await Permission.create({ slug: "order:view", description: "Ver pedidos" });
        const p3 = await Permission.create({ slug: "report:view", description: "Ver reportes" });
        const p4 = await Permission.create({ slug: "fleet:update", description: "Actualizar flota" });

        // 2. Crear Roles
        const roleAdmin = await Role.create({ name: "ADMIN" });
        const roleDriver = await Role.create({ name: "DRIVER" });
        const roleClient = await Role.create({ name: "CLIENT" });
        const roleSuper = await Role.create({ name: "SUPERVISOR" });

        // 3. Asignar Permisos a Roles (Magia de Sequelize)
        await roleAdmin.addPermissions([p1, p2, p3, p4]); // Admin hace todo
        await roleDriver.addPermissions([p2, p4]);       // Driver ve pedidos y actualiza flota
        await roleClient.addPermissions([p1, p2]);       // Cliente crea y ve pedidos

        // 4. Crear Zonas
        await Zone.create({ nombre_ciudad: "Quito", zona_geo: "NORTE" });
        await Zone.create({ nombre_ciudad: "Quito", zona_geo: "SUR" });
        await Zone.create({ nombre_ciudad: "Quito", zona_geo: "CENTRO" });
        await Zone.create({ nombre_ciudad: "Guayaquil", zona_geo: "NORTE" });

        console.log("Seed completado con éxito.");
        process.exit();
    } catch (error) {
        console.error("Error en el seed:", error);
        process.exit(1);
    }
}

seed();