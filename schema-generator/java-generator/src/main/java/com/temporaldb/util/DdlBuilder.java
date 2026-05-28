package com.temporaldb.util;

import com.temporaldb.model.Attribute;
import com.temporaldb.model.Entity;
import com.temporaldb.model.KeyAttribute;
import com.temporaldb.model.TimeDimension;

import java.util.List;

public class DdlBuilder {

    public static String createBaseTable(Entity e) {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE `").append(e.getName()).append("` (\n");
        
        List<KeyAttribute> keys = e.getPrimaryKey().getKeyAttributes();
        for (KeyAttribute key : keys) {
            sb.append("  `").append(key.getName()).append("` ")
              .append(mapType(key.getType())).append(" NOT NULL,\n");
        }

        if (e.getAttributes() != null) {
            for (Attribute a : e.getAttributes()) {
                if (!a.isTemporal()) {
                    sb.append("  `").append(a.getName()).append("` ")
                      .append(mapType(a.getType())).append(",\n");
                }
            }
        }
        
        sb.append("  PRIMARY KEY (");
        for (int i = 0; i < keys.size(); i++) {
            sb.append("`").append(keys.get(i).getName()).append("`");
            if (i < keys.size() - 1) sb.append(", ");
        }
        sb.append(")\n) ENGINE=InnoDB;\n\n");
        return sb.toString();
    }

    public static String createHistoryTable(Entity e, TimeDimension td) {
        String hist = e.getName() + "_history";
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE `").append(hist).append("` (\n");

        // Surrogate key – uniquely identifies each history row
        sb.append("  `history_id` INT AUTO_INCREMENT PRIMARY KEY,\n");

        // Natural / mapping key(s) (e.g. ssn, dnumber, essn+pno)
        List<KeyAttribute> keys = e.getPrimaryKey().getKeyAttributes();
        if (keys != null) {
            for (KeyAttribute key : keys) {
                sb.append("  `").append(key.getName()).append("` ")
                  .append(mapType(key.getType())).append(" NOT NULL,\n");
            }
        }

        // Temporal attributes
        if (e.getAttributes() != null) {
            for (Attribute a : e.getAttributes()) {
                sb.append("  `").append(a.getName()).append("` ")
                  .append(mapType(a.getType())).append(",\n");
            }
        }

        // valid_from is always required; valid_to is NULL when the record is still current
        sb.append("  `").append(td.getValidStartField()).append("` DATETIME NOT NULL,\n");
        sb.append("  `").append(td.getValidEndField()).append("` DATETIME NULL DEFAULT NULL,\n");

        // Single transaction timestamp – when this version was recorded in the DB
        sb.append("  `").append(td.getTransactionField()).append("` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP\n");

        sb.append(") ENGINE=InnoDB;\n\n");
        return sb.toString();
    }

    private static String mapType(String xmlType) {
        if (xmlType == null) return "VARCHAR(255)";
        switch (xmlType.toUpperCase()) {
            case "STRING": return "VARCHAR(255)";
            case "INTEGER": return "INT";
            case "FLOAT": return "FLOAT";
            case "DOUBLE": return "DOUBLE";
            case "DATE": return "DATE";
            case "DATETIME": return "DATETIME";
            default: return "VARCHAR(255)";
        }
    }
}
