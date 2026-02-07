import { DataTypes } from "sequelize";
import { sequelize } from "../config/db.js";

const Permission = sequelize.define("Permission", {
    id: {
        type: DataTypes.INTEGER,
        primaryKey: true,
        autoIncrement: true
    },
    slug: {
        type: DataTypes.STRING,
        unique: true,
        allowNull: false
    },
    description: {
        type: DataTypes.STRING
    }
}, {
    tableName: 'permissions',
    timestamps: false
});

export default Permission;