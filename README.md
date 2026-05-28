# Temporal Database System Backend

A modern Java Spring Boot and MySQL-backed application implementing a **Temporal Database System** with automatic schema-driven history tracking. 

The project supports **Valid Time (Temporal DB)** versioning, enabling temporal joins, time slices, and queries about past states of data (such as querying an employee's manager during a specific salary window).

---

## 📂 Project Structure

*   **`src/`**: The core Spring Boot REST API application. It implements the controller, DTO, service, and utility classes for processing dynamic temporal operations.
*   **`schema-generator/`**: Contains the tools and configurations defining the database.
    *   `companydb.sql`: The base relational schema setup script.
    *   `companydb_schema.xml`: The XML configuration defining temporal metadata policies, primary keys, and temporal attributes.
    *   `java-generator/`: A Maven tool that uses JAXB to parse the XML metadata schema and automatically generate history tables in `schema.sql`.
*   **`docs/`**: Documentation and architecture workflows.
    *   `schema-generator-workflow.md`: Deep-dive into JAXB parsing and DDL compilation.
    *   `temporal-concepts.md`: Theoretical guide explaining Valid Time, Transaction Time, and state transitions.
    *   `api-guide.md`: Full reference guide containing cURL commands and JSON request/response payloads.
    *   `spring-reference-guide.md`: General Spring-related documentation and resources.

---

## 🛠️ Setup & Execution

### 1. Database Setup
1.  Initialize a MySQL database schema named `companydb` (or as configured in your application properties).
2.  Import and execute `schema-generator/companydb.sql` to build the standard, non-temporal relational database tables.

### 2. Generate Temporal History Schema
If you change `companydb_schema.xml` or need to regenerate the history tables:
1.  Navigate into `schema-generator/java-generator/`:
    ```bash
    cd schema-generator/java-generator
    ```
2.  Compile and execute the schema compiler:
    ```bash
    mvn compile exec:java -Dexec.mainClass="com.temporaldb.GenerateSchema"
    ```
3.  This compiles the JAXB models and writes the output statements into `schema-generator/java-generator/schema.sql`.
4.  Execute `schema.sql` in your MySQL database to create the temporal history tables (e.g., `Employee_history`, `Department_history`, `works_on_history`).

### 3. Running the Backend
1.  Edit `src/main/resources/application.properties` with your database URL, username, and password.
2.  Compile and run the Spring Boot application from the root folder:
    ```bash
    mvn spring-boot:run
    ```
3.  The API will start locally on `http://localhost:8080`.

---

## 🔌 API Documentation

### A. Core CRUD Operations (Automatic History Management)
All CRUD actions automatically manage matching records in the history tables.

*   `POST /api/employee` — Create a new employee (opens a valid timeline state).
*   `PUT /api/employee/{ssn}` — Update an employee. Automatically closes the old history record and opens a new version.
*   `DELETE /api/employee/{ssn}` — Remove an employee from active tracking and close their history window.
*   *(Similar endpoints exist for `/api/department` and `/api/works-on`)*

### B. Standard History Queries
Fetch previous states of a specific record by its primary key:
*   `GET /api/employee/{ssn}/history/first` — Fetch the first recorded state.
*   `GET /api/employee/{ssn}/history/last` — Fetch the most recent recorded state.
*   `GET /api/employee/{ssn}/history/previous` — Fetch the state immediately preceding the current active state.

### C. Advanced Temporal & Correlated Queries
*   `GET /api/temporal/{ssn}/manager/salary/{salary}`
    *   **Logic**: Uses `executeWhen` to query who was the manager of a given employee during the period when that employee earned a specific salary.
*   `GET /api/temporal/{ssn}/works-on/{pno}/salary/{salary}`
    *   **Logic**: Uses `executeTimeSlice` to perform a temporal join, calculating the hours spent on project `pno` by the employee's manager during the time the subordinate earned a specific salary.
