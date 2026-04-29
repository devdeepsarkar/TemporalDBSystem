# Java Generator Workflow

This document explains the architecture and workflow of the Java-based Temporal Database Schema Generator.

## 1. High-Level Architecture
The goal of the Java Generator is to read the configuration defined in `Companydb_Schema_XML` and automatically produce a valid MySQL relational schema (`schema.sql`) that supports temporal tracking via history tables.

The system is built on **JAXB (Jakarta XML Binding)**, which is a Java standard for mapping XML elements directly into Java Objects (POJOs). This allows the application to interact with the XML structure using strongly-typed Java code rather than manually traversing XML nodes.

The execution pipeline consists of three phases:
1. **Unmarshalling (Parsing):** JAXB reads the XML file and instantiates a tree of Java objects (`TemporalDatabaseConfig`, `Entity`, `Attribute`, etc.) representing the desired database schema.
2. **DDL Construction:** The generator iterates through the mapped objects and uses a builder utility to construct raw MySQL `CREATE TABLE` statements as strings.
3. **Database Export:** The generated SQL script is written to the local filesystem (`schema.sql`), ready to be executed against the target MySQL database.

---

## 2. Step-by-Step Execution Flow

1. **Initialization:** The user runs the packaged JAR file (`java -jar schema-generator-1.0.0-jar-with-dependencies.jar`).
2. **Context Setup:** `GenerateSchema.main()` creates a `JAXBContext` tailored to the `TemporalDatabaseConfig` class.
3. **XML Reading:** The `Unmarshaller` reads `Companydb_Schema_XML`. It matches XML tags to Java fields based on annotations (e.g., `@XmlElement`, `@XmlAttribute`).
4. **Validation:** If the XML is malformed or violates the declared namespace (`http://www.example.com/temporaldb`), the unmarshaller throws an exception.
5. **Iteration:** For each parsed `Entity`, the script checks if `temporal="true"`.
6. **SQL Generation:** If temporal, it passes the `Entity` and the global `TimeDimension` configuration to `DdlBuilder.createHistoryTable()`.
7. **Type Mapping:** `DdlBuilder` maps XML data types (like `STRING`, `FLOAT`) to MySQL data types (like `VARCHAR(255)`, `FLOAT`).
8. **File Writing:** The accumulated SQL strings are concatenated and saved to `schema.sql`.

---

## 3. Detailed File Breakdown

### Execution & Utilities
* **`pom.xml`**
  The Maven Project Object Model. It defines the project metadata, Java version (17), and fetches the JAXB dependencies required for XML parsing. Crucially, it configures the `maven-assembly-plugin` to bundle the generator and all dependencies into a single "fat JAR" for easy execution.

* **`GenerateSchema.java`**
  The main entry point of the application. It orchestrates the entire workflow: finding the XML file, initiating the JAXB unmarshalling process, looping over the configured entities, invoking the SQL builder, and writing the final `schema.sql` to disk.

* **`util/DdlBuilder.java`**
  The SQL construction engine. It encapsulates the logic for formatting `CREATE TABLE` statements. It is responsible for:
  * Creating the table structure and naming the tables (e.g., appending `_history`).
  * Mapping primary keys and attributes.
  * Injecting the four temporal columns (`valid_from`, `valid_to`, `tx_from`, `tx_to`) dynamically based on the `<TimeDimension>` configuration.
  * Enforcing the correct composite Primary Key for history tables (`original_pk`, `valid_from`, `tx_from`).

### The Data Model (JAXB Mapping)
The `com.temporaldb.model` package serves as a 1:1 blueprint of the XML configuration.

* **`package-info.java`**
  A critical file that declares the package-level XML namespace (`http://www.example.com/temporaldb`). It tells JAXB that all XML elements mapped by classes in this package belong to this specific namespace.

* **`TemporalDatabaseConfig.java`**
  The root object representing `<TemporalDatabaseConfig>`. It acts as the container holding the list of `Entity` objects and the `TimeDimension` settings.

* **`Entity.java`**
  Maps the `<Entity>` tag. It reads the entity's `name` and `temporal` status. It also safely holds references to its `PrimaryKey` and a list of internal `Attribute` instances.

* **`PrimaryKey.java` & `KeyAttribute.java`**
  Maps the `<PrimaryKey>` block. Because primary keys can be complex, these classes extract the underlying column `name` and `type` specifically marked as the primary identifier (e.g., `ssn`, `dnumber`).

* **`Attribute.java`**
  Maps individual `<Attribute>` tags within an entity. It captures the column `name`, `type` (e.g., `FLOAT`, `INTEGER`), and whether the specific column expects temporal tracking.

* **`TimeDimension.java`**
  Maps the `<TimeDimension>` tag. It stores the user-defined column names for the temporal axes (valid time and transaction time). Used by the `DdlBuilder` to ensure the generated SQL uses the exact column names specified in the XML config.

---

## 4. Key Advantages of this Architecture
* **Extensibility:** If the XML schema introduces new tags (e.g., `<Relationships>`), you only need to add a new Java class using JAXB annotations. The rest of the pipeline remains unaffected.
* **Type Safety:** By parsing XML into Java Objects, the DDL Builder operates on guaranteed, strongly-typed data rather than fragile string-parsing or regex.
* **Separation of Concerns:** The XML parsing (Model) is strictly decoupled from the SQL generation (Builder), making it easy to swap MySQL output for PostgreSQL or Oracle DDL in the future.
