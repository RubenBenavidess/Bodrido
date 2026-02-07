import { login as loginService, logout as logoutService, register as registerService, refreshToken as refreshTokenService } from "../services/authService.js";
import { loginSchema, registerSchema } from "../security/zSchemes.js"; // Asegurate de exportar registerSchema en zSchemes
import { validateToken } from "../security/jwtManager.js";

export async function register(req, res, next) {
    try {
        // 1. Validar datos de entrada con Zod
        const userData = registerSchema.parse(req.body);
        
        // 2. Llamar al servicio
        const newUser = await registerService(userData);
        
        return res.status(201).json({
            success: true,
            message: "User registered successfully",
            data: {
                id: newUser.id,
                username: newUser.username,
                email: newUser.email
            }
        });
    } catch (e) {
        return next(e);
    }
}

export async function login(req, res, next) {
    try {
        // 1. Validar
        const credentials = loginSchema.parse(req.body);
        
        // 2. Servicio (Ahora devuelve accessToken, refreshToken y user)
        const result = await loginService(credentials);

        // 3. Cookie para el Refresh Token (Más seguro que enviarlo solo en JSON)
        // Ojo: El PDF pide devolver JWT, lo mandamos en el body también por si acaso (para móviles)
        res.cookie("refreshToken", result.refreshToken, {
            httpOnly: true,
            secure: false, // TRUE en producción (HTTPS)
            sameSite: "Lax",
            maxAge: 7 * 24 * 60 * 60 * 1000 // 7 días
        });

        // 4. Respuesta JSON requerida
        return res.status(200).json({
            success: true,
            message: "Authenticated",
            accessToken: result.accessToken, // Este se usa en el Header: Authorization Bearer ...
            refreshToken: result.refreshToken, 
            user: result.user
        });
    } catch (e) {
        return next(e);
    }
}

export async function refreshToken(req, res, next) {
    try {
        // Buscamos el token en Cookie O en el Body
        const tokenString = req.cookies.refreshToken || req.body.refreshToken;
        
        if (!tokenString) {
            return res.status(400).json({ success: false, message: "Refresh Token required" });
        }

        const result = await refreshTokenService(tokenString);

        // Actualizamos la cookie con el nuevo refresh token rotado
        res.cookie("refreshToken", result.refreshToken, {
            httpOnly: true,
            secure: false,
            sameSite: "Lax",
            maxAge: 7 * 24 * 60 * 60 * 1000
        });

        return res.status(200).json({
            success: true,
            accessToken: result.accessToken,
            refreshToken: result.refreshToken
        });

    } catch (e) {
        return next(e);
    }
}

export async function logout(req, res, next) {
    try {
        const tokenString = req.cookies.refreshToken || req.body.refreshToken;
        
        if (tokenString) {
            await logoutService(tokenString);
        }
        
        res.clearCookie("refreshToken");
        // Opcional: Limpiar access token si lo guardabas en cookie
        res.clearCookie("accessToken"); 
        
        return res.status(200).json({
            success: true,
            message: "Logged out successfully"
        });
    } catch (e) {
        return next(e);
    }
}

// verifySession se queda igual o lo adaptas para validar el header Authorization
export async function verifySession(req, res, next) {
    try {
        // Ahora validamos el Header, no la cookie (Standard REST)
        const authHeader = req.headers.authorization;
        const token = authHeader && authHeader.split(' ')[1]; // "Bearer eyJ..."

        if (!token) throw new Error("No token provided");

        const decoded = validateToken(token);
        
        return res.status(200).json({
            success: true,
            user: decoded
        });
    } catch (e) {
        return res.status(401).json({ success: false, message: "Invalid Session" });
    }
}