import { z } from "zod";

export const loginSchema = z.object({
    username: z.string().min(1),
    password: z.string().min(6)
}).strict();

export const registerSchema = z.object({
    username: z.string().min(3),
    email: z.string().email(),
    password: z.string().min(6),
    role: z.enum(["CLIENT", "DRIVER", "ADMIN", "SUPERVISOR"]), // Roles permitidos
    vehicle_type: z.enum(["MOTORCYCLE", "LIGHT_VEHICLE", "TRUCK"]).optional(),
    zone_id: z.number().int().optional()
}).strict();