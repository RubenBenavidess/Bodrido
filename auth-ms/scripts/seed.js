// src/scripts/seed.js
import { sequelize } from "../config/db.js";
import Role from "../models/Role.js";
import Permission from "../models/Permission.js";
import Zone from "../models/Zone.js";
import defineAssociations from "../models/associations.js";

async function seed() {
    try {
        await sequelize.authenticate();
        defineAssociations();
        await sequelize.sync({ force: true });

        console.log("Iniciando Seed...");

        // 1. Crear Permisos
        // order:view -> Ahora será "Ver TODOS los pedidos" (Admin)
        const p_view_all = await Permission.create({ slug: "order:view", description: "Ver todos los pedidos" });
        
        // NUEVO PERMISO: order:view_own -> Para que el cliente vea SUS pedidos
        const p_view_own = await Permission.create({ slug: "order:view_own", description: "Ver mis pedidos" });

        const p_create = await Permission.create({ slug: "order:create", description: "Crear pedidos" });
        const p_update = await Permission.create({ slug: "order:update", description: "Actualizar pedidos" });
        const p_view_nopicked = await Permission.create({ slug: "order:view_nopicked", description: "Ver pedidos no recogidos" });
        
        // Permisos de Flota
        const p_fleet_create = await Permission.create({ slug: "fleet:create", description: "Crear flota" });
        const p_fleet_update = await Permission.create({ slug: "fleet:update", description: "Actualizar flota" });
        const p_fleet_view = await Permission.create({ slug: "fleet:view", description: "Ver flota" });

        // 2. Crear Roles
        const roleAdmin = await Role.create({ name: "ADMIN" });
        const roleDriver = await Role.create({ name: "DRIVER" });
        const roleClient = await Role.create({ name: "CLIENT" });
        const roleSuper = await Role.create({ name: "SUPERVISOR" });

        // 3. Asignar Permisos
        // ADMIN: Acceso total a órdenes, creación de pedidos, actualización de pedidos, flota completa
        // Ver: Todas las órdenes, sus propias órdenes, no recogidas, flota
        // Crear: Órdenes, conductores, vehículos
        // Actualizar: Órdenes, conductores, vehículos
        await roleAdmin.addPermissions([p_view_all, p_view_own, p_create, p_update, p_view_nopicked, p_fleet_create, p_fleet_update, p_fleet_view]);
        
        // SUPERVISOR: Opera la plataforma (excepto ver datos de clientes individuales)
        // Ver: Todas las órdenes, órdenes no recogidas, flota
        // Crear: Conductores, vehículos
        // Actualizar: Órdenes, conductores, vehículos
        await roleSuper.addPermissions([p_view_all, p_update, p_view_nopicked, p_fleet_create, p_fleet_update, p_fleet_view]);

        // DRIVER: Puede ver la flota de conductores y vehículos para consulta
        // Ver: Órdenes no recogidas, flota
        // NO PUEDE: Crear u actualizar
        await roleDriver.addPermissions([p_view_nopicked, p_fleet_view]);

        // CLIENTE: Crea sus propias órdenes y ve solo sus datos
        // Ver: Sus propias órdenes
        // Crear: Ordenes
        // NO PUEDE: Ver todas las órdenes, actualizar órdenes, acceder a flota
        await roleClient.addPermissions([p_create, p_view_own]);

        // 4. Crear Zonas
        await Zone.create({ nombre_ciudad: "Quito", zona_geo: "NORTE" });
        await Zone.create({ nombre_ciudad: "Quito", zona_geo: "SUR" });
        await Zone.create({ nombre_ciudad: "Quito", zona_geo: "CENTRO" });
        await Zone.create({ nombre_ciudad: "Guayaquil", zona_geo: "NORTE" });

        console.log("Seed completado. Permisos actualizados correctamente.");
        process.exit();
    } catch (error) {
        console.error("Error en el seed:", error);
        process.exit(1);
    }
}

seed();