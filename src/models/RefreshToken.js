import { DataTypes } from "sequelize";
import { sequelize } from "../config/db.js";

const RefreshToken = sequelize.define("RefreshToken", {
    id: {
        type: DataTypes.INTEGER,
        primaryKey: true,
        autoIncrement: true
    },
    token_hash: {
        type: DataTypes.STRING,
        allowNull: false
    },
    expires_at: {
        type: DataTypes.DATE,
        allowNull: false
    },
    revoked: {
        type: DataTypes.BOOLEAN,
        defaultValue: false
    },
    replaced_by_token: {
        type: DataTypes.STRING
    }
}, {
    tableName: 'refresh_tokens',
    updatedAt: false,
    createdAt: 'created_at'
});

export default RefreshToken;