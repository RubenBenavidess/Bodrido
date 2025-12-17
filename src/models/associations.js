import User from "./User.js";
import Role from "./Role.js";
import Permission from "./Permission.js";
import Zone from "./Zone.js";
import RefreshToken from "./RefreshToken.js";

export default function defineAssociations() {
    // Un Usuario tiene un Rol
    Role.hasMany(User, { foreignKey: 'role_id' });
    User.belongsTo(Role, { foreignKey: 'role_id' });

    // Un Usuario pertenece a una Zona (opcional)
    Zone.hasMany(User, { foreignKey: 'zone_id' });
    User.belongsTo(Zone, { foreignKey: 'zone_id' });

    // Roles y Permisos (Muchos a Muchos) -> Crea la tabla 'role_permissions' sola
    Role.belongsToMany(Permission, { through: 'role_permissions', foreignKey: 'role_id', otherKey: 'permission_id' });
    Permission.belongsToMany(Role, { through: 'role_permissions', foreignKey: 'permission_id', otherKey: 'role_id' });

    // Refresh Tokens
    User.hasMany(RefreshToken, { foreignKey: 'user_id' });
    RefreshToken.belongsTo(User, { foreignKey: 'user_id' });
}