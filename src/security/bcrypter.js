import bcrypt from "bcrypt";

// Función para encriptar la contraseña (hashPassword)
export async function hashPassword(password) {
    const salt = await bcrypt.genSalt(10);
    return await bcrypt.hash(password, salt);
}

// Función para comparar contraseñas (compareHash)
export async function compareHash(password, hash) {
    return await bcrypt.compare(password, hash);
}