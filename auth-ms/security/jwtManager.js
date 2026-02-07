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

  let scope = "read"; 

  if (user.Role && user.Role.Permissions) {
    scope = user.Role.Permissions.map(p => p.slug).join(" ");
  }

  // AQUÍ ESTÁ LA MAGIA DEL PDF:
  const payload = {
      sub: user.username,           // Subject estándar
      user_id: user.id,             // Útil para logs
      role: user.role,              // Requerido
      scope: scope,                 // Requerido 
      zone_id: user.zone_id,        // Requerido
      fleet_type: user.vehicle_type // Requerido
  };  

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