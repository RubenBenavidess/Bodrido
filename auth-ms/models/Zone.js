import { DataTypes } from "sequelize";
import { sequelize } from "../config/db.js";
import { ZONES_GEO } from "../config/enums.js";

const Zone = sequelize.define("Zone", {
    id: {
        type: DataTypes.INTEGER,
        primaryKey: true,
        autoIncrement: true
    },
    nombre_ciudad: {
        type: DataTypes.STRING,
        allowNull: false
    },
    zona_geo: {
        type: DataTypes.ENUM(...Object.values(ZONES_GEO)),
        allowNull: false
    }
}, {
    tableName: 'zones',
    timestamps: false 
});

export default Zone;