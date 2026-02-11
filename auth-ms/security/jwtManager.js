import jwt from "jsonwebtoken";
import fs from "fs";
import dotenv from "dotenv";

dotenv.config();

function loadKey(envPathVar) {
  const path = process.env[envPathVar];
  if (path) {
    try {
        // Asegúrate que las rutas sean absolutas o relativas desde la raíz del proyecto
        return fs.readFileSync(path, "utf8");
    } catch (err) {
      console.error(`Error leyendo clave desde ${path}:`, err.message);
      throw err;
    }
  }
  return null;
}

const PRIVATE_KEY = loadKey("PRIVATE_KEY_PATH");
const PUBLIC_KEY = loadKey("PUBLIC_KEY_PATH");

if (!PRIVATE_KEY || !PUBLIC_KEY) {
  console.warn("JWT keys not found. Check env vars PRIVATE_KEY_PATH / PUBLIC_KEY_PATH");
}

export function generateToken(user, options = {}) {
  if (!PRIVATE_KEY) throw new Error("Private Key Not Found");

  // Extraer permisos y construir scopes
  let scope = "read"; // Scope por defecto

  if (user.Role) {
    const roleName = user.Role.name || user.Role;
    
    // Si es ADMIN, dar acceso total
    if (roleName === "ADMIN") {
      scope = "order:view order:view_own order:create order:update order:view_nopicked fleet:create fleet:update fleet:view";
    } 
    // Si es SUPERVISOR
    else if (roleName === "SUPERVISOR") {
      scope = "order:view order:update order:view_nopicked fleet:create fleet:update fleet:view";
    }
    // Si es DRIVER
    else if (roleName === "DRIVER") {
      scope = "order:view_nopicked fleet:view";
    }
    // Si es CLIENT
    else if (roleName === "CLIENT") {
      scope = "order:create order:view_own";
    }
    // Si tiene Permissions array (desde include)
    else if (user.Role.Permissions && Array.isArray(user.Role.Permissions)) {
      scope = user.Role.Permissions.map(p => p.slug).join(" ");
      console.log(`>>> [JWT] Scopes extraídos del rol ${roleName}:`, scope);
    }
  }

  // AQUÍ ESTÁ LA MAGIA DEL PDF:
  const payload = {
      sub: user.username,                    // Subject estándar
      user_id: user.id,                      // Útil para logs
      role: user.Role?.name || "CLIENT",     // Nombre del rol, no undefined
      scope: scope,                          // Requerido ✓
      zone_id: user.zone_id,                 // Requerido
      fleet_type: user.vehicle_type          // Requerido
  };

  console.log(`>>> [JWT] Generando token para ${user.username} con scopes:`, scope);

  const token = jwt.sign(payload, PRIVATE_KEY, {
    expiresIn: options.expiresIn || "1h",
    algorithm: "ES256"
  });
  return token;
}

export function validateToken(token) {
  if (!PUBLIC_KEY) throw new Error("Public Key Not Found");
  const decoded = jwt.verify(token, PUBLIC_KEY, {
    algorithms: ["ES256"]
  });
  return decoded;
}