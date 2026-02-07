// src/services/authService.js
import User from "../models/User.js";
import Role from "../models/Role.js";
import Permission from "../models/Permission.js";
import Zone from "../models/Zone.js";
import RefreshToken from "../models/RefreshToken.js";
import { generateToken } from "../security/jwtManager.js";
import { sequelize } from "../config/db.js";
import crypto from "crypto";

// Función auxiliar para crear tokens opacos (random string)
function generateRefreshTokenString() {
    return crypto.randomBytes(40).toString('hex');
}

export async function register(userData) {
    // Usamos una transacción por seguridad
    const t = await sequelize.transaction();
    try {
        // 1. Buscar el rol
        // Si el frontend manda el nombre del rol (ej: "DRIVER"), hay que buscar su ID
        let roleId = userData.role_id;
        if (!roleId && userData.role) {
            const role = await Role.findOne({ where: { name: userData.role } });
            if (!role) throw new Error("Role not found");
            roleId = role.id;
        }

        const newUser = await User.create({
            username: userData.username,
            email: userData.email,
            password_hash: userData.password, // El hook lo hasheará
            role_id: roleId,
            vehicle_type: userData.vehicle_type || null,
            zone_id: userData.zone_id || null
        }, { transaction: t });

        await t.commit();
        return newUser;
    } catch (error) {
        await t.rollback();
        throw error;
    }
}

export async function login(credentials) {
    // 1. Buscar usuario inteligentemente con su username
    const user = await User.findOne({
        where: { username: credentials.username },
        include: [
            { 
                model: Role, 
                include: [Permission] // Traer permisos para el scope
            },
            { model: Zone } // Traer zona para el zone_id
        ]
    });

    if (!user) throw new Error("Invalid Credentials");

    const isValid = await user.comparePass(credentials.password);
    if (!isValid) throw new Error("Invalid Credentials");

    // 2. Generar JWT (Access Token)
    // El jwtManager internamente extraerá role, scope, zone, fleet del objeto user
    const accessToken = generateToken(user);

    // 3. Generar Refresh Token (Opaque Token) y guardarlo en BDD
    const refreshTokenString = generateRefreshTokenString();
    
    // Calcular expiración (ej: 7 días)
    const expiresAt = new Date();
    expiresAt.setDate(expiresAt.getDate() + 7);

    await RefreshToken.create({
        user_id: user.id,
        token_hash: refreshTokenString, // En producción deberías hashear esto también
        expires_at: expiresAt,
        revoked: false
    });

    return {
        accessToken,
        refreshToken: refreshTokenString,
        user: {
            username: user.username,
            role: user.Role.name,
            email: user.email
        }
    };
}

export async function refreshToken(tokenString) {
    // 1. Buscar el token en BDD
    const storedToken = await RefreshToken.findOne({
        where: { token_hash: tokenString },
        include: [
            { 
                model: User,
                include: [{ model: Role, include: [Permission] }, { model: Zone }]
            }
        ]
    });

    // 2. Validaciones
    if (!storedToken) throw new Error("Invalid Refresh Token");
    if (storedToken.revoked) {
        // Opcional: Revocar todos los tokens de este usuario por seguridad (robo de token)
        throw new Error("Token Revoked");
    }
    if (new Date() > storedToken.expires_at) {
        throw new Error("Token Expired");
    }

    // 3. Rotación de Tokens (Seguridad)
    // Revocamos el token viejo y damos uno nuevo (Refresh Token Rotation)
    const t = await sequelize.transaction();
    try {
        storedToken.revoked = true;
        storedToken.replaced_by_token = "new_generated"; // Marca
        await storedToken.save({ transaction: t });

        // Generar nuevo par
        const newAccessToken = generateToken(storedToken.User);
        const newRefreshTokenString = generateRefreshTokenString();
        
        const expiresAt = new Date();
        expiresAt.setDate(expiresAt.getDate() + 7);

        await RefreshToken.create({
            user_id: storedToken.user_id,
            token_hash: newRefreshTokenString,
            expires_at: expiresAt
        }, { transaction: t });

        await t.commit();

        return {
            accessToken: newAccessToken,
            refreshToken: newRefreshTokenString
        };

    } catch (error) {
        await t.rollback();
        throw error;
    }
}

export async function logout(tokenString) {
    // Revocar el token en BDD
    await RefreshToken.update(
        { revoked: true },
        { where: { token_hash: tokenString } }
    );
    return true;
}