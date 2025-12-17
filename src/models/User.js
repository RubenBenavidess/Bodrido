import { DataTypes } from "sequelize";
import { sequelize } from "../config/db.js";
import { hashPassword, compareHash } from "../security/bcrypter.js";
import { VEHICLE_TYPES } from "../config/enums.js";


const User = sequelize.define("User", {
    id: {
        type: DataTypes.UUID,
        defaultValue: DataTypes.UUIDV4,
        primaryKey: true
    },
    username: {
        type: DataTypes.STRING,
        unique: true,
        allowNull: false
    },
    email: {
        type: DataTypes.STRING,
        unique: true,
        allowNull: false
    },
    password_hash: { // Tu esquema dice password_hash, respetémoslo
        type: DataTypes.STRING,
        allowNull: false
    },
    is_active: {
        type: DataTypes.BOOLEAN,
        defaultValue: true
    },

    role_id: {
        type: DataTypes.INTEGER,
        allowNull: false,
    },

    vehicle_type: {
        type: DataTypes.ENUM(...Object.values(VEHICLE_TYPES)),
        allowNull: true
    },

    zone_id: {
        type: DataTypes.INTEGER,
        allowNull: true,
    }
    
}, {
    tableName: 'users',
    updatedAt: false,
    createdAt: 'created_at',
    hooks: {
        beforeCreate: async (user) => {
            if (user.password_hash) {
                user.password_hash = await hashPassword(user.password_hash);
            }
        }
    }
});

// Método para comparar pass
User.prototype.comparePass = async function(possiblePass) {
    return await compareHash(possiblePass, this.password_hash);
};

export default User;