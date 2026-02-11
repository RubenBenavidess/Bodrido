import User from "../models/User.js";

export async function deactivateUserDueToCustomerCreationFailure(event) {
    try {
        const user = await User.findOne({
            where: { id: event.user_id }
        });
        
        if (!user) {
            console.warn(`⚠️ Usuario no encontrado para desactivar: userId=${event.user_id}`);
            return false;
        }
        
        // Desactivar usuario
        user.is_active = false;
        await user.save();
        
        console.log(`✓ Usuario desactivado por fallo de customer-ms: userId=${event.user_id}, email=${event.email}`);
        console.log(`  Razón: ${event.reason}`);
        
        return true;
    } catch (error) {
        console.error(`✗ Error desactivando usuario: userId=${event.user_id}, error=${error.message}`, error);
        throw error;
    }
}
