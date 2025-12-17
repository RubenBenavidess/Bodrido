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
        const p2 = await Permission.create({ slug: "order:update", description: "Actualizar pedidos" });
        const p3 = await Permission.create({ slug: "order:view", description: "Ver pedidos" });
        const p4 = await Permission.create({ slug: "order:view_nopicked", description: "Ver pedidos no recogidos" });
        const p5 = await Permission.create({ slug: "fleet:create", description: "Crear flota" });
        const p6 = await Permission.create({ slug: "fleet:update", description: "Actualizar flota" });
        const p7 = await Permission.create({ slug: "fleet:view", description: "Ver flota" });

        // 2. Crear Roles
        const roleAdmin = await Role.create({ name: "ADMIN" });
        const roleDriver = await Role.create({ name: "DRIVER" });
        const roleClient = await Role.create({ name: "CLIENT" });
        const roleSuper = await Role.create({ name: "SUPERVISOR" });

        // 3. Asignar Permisos a Roles (Magia de Sequelize)
        await roleAdmin.addPermissions([p1, p2, p3, p4, p5, p6, p7]); // Admin hace todo
        await roleSuper.addPermissions([p2, p3, p5, p6]);      // Supervisor ve y actualiza pedidos y flota
        await roleDriver.addPermissions([p4]);              // Driver ve pedidoss no recogidos
        await roleClient.addPermissions([p1, p3]);       // Cliente crea y ve pedidos

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