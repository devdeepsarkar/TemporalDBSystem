package com.temporaldb;

import jakarta.xml.bind.*;
import java.nio.file.*;
import com.temporaldb.model.TemporalDatabaseConfig;
import com.temporaldb.model.Entity;
import com.temporaldb.util.DdlBuilder;

public class GenerateSchema {
    public static void main(String[] args) throws Exception {
        // Read XML file. Since we run from java-generator, schema is one level up
        Path xmlPath = Paths.get("../Companydb_Schema_XML");
        if (!Files.exists(xmlPath)) {
            // fallback if running from the project root instead
            xmlPath = Paths.get("Companydb_Schema_XML");
        }

        JAXBContext ctx = JAXBContext.newInstance(TemporalDatabaseConfig.class);
        Unmarshaller um = ctx.createUnmarshaller();
        TemporalDatabaseConfig cfg = (TemporalDatabaseConfig) um.unmarshal(xmlPath.toFile());

        StringBuilder ddl = new StringBuilder();
        ddl.append("-- Auto-generated from Companydb_Schema_XML\n\n");

        if (cfg.getEntities() != null) {
            for (Entity e : cfg.getEntities()) {
                // Base tables already exist; only generate history tables.
                if (e.isTemporal()) {
                    ddl.append(DdlBuilder.createHistoryTable(e, cfg.getTimeDimension()));
                }
            }
        }

        // Write output
        Path outPath = Paths.get("schema.sql");
        Files.writeString(outPath, ddl.toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        System.out.println("Generated SQL written to: " + outPath.toAbsolutePath());
    }
}
