# API Reference & Usage Guide

This guide provides concrete `cURL` request and JSON payload examples for the Temporal Backend REST API.

---

## 🚀 Base URL
All API requests are made relative to:
```
http://localhost:8080
```

---

## 1. Core CRUD API (Automatic History Updates)

### A. Create an Employee
When a record is inserted here, a history record is automatically opened with `valid_from = NOW()` and `valid_to = NULL`.

*   **Request**: `POST /api/employee`
*   **Body**:
    ```json
    {
      "ssn": "123456789",
      "fname": "John",
      "minit": "B",
      "lname": "Smith",
      "bdate": "1985-06-15",
      "address": "123 Main St, Houston, TX",
      "sex": "M",
      "salary": 55000.0,
      "super_ssn": "888665555",
      "dno": 5
    }
    ```
*   **cURL Example**:
    ```bash
    curl -X POST http://localhost:8080/api/employee \
      -H "Content-Type: application/json" \
      -d '{"ssn":"123456789","fname":"John","minit":"B","lname":"Smith","bdate":"1985-06-15","address":"123 Main St, Houston, TX","sex":"M","salary":55000.0,"super_ssn":"888665555","dno":5}'
    ```

### B. Update an Employee's Salary (Triggers Timeline Split)
Updating a temporal attribute (like `salary` or `dno`) closes the active history record by setting `valid_to = NOW()` and opens a new history row with the new values.

*   **Request**: `PUT /api/employee/123456789`
*   **Body**:
    ```json
    {
      "salary": 65000.0
    }
    ```
*   **cURL Example**:
    ```bash
    curl -X PUT http://localhost:8080/api/employee/123456789 \
      -H "Content-Type: application/json" \
      -d '{"salary":65000.0}'
    ```

### C. Delete an Employee (Closes Active History)
Deletes the active record from the main table, and updates `valid_to = NOW()` in the history table.

*   **Request**: `DELETE /api/employee/123456789`
*   **cURL Example**:
    ```bash
    curl -X DELETE http://localhost:8080/api/employee/123456789
    ```

---

## 2. Querying Historical States

### A. Fetch First Recorded State
Get the oldest recorded database state for an employee.
*   **Request**: `GET /api/employee/{ssn}/history/first`
*   **cURL Example**:
    ```bash
    curl http://localhost:8080/api/employee/123456789/history/first
    ```
*   **Response**:
    ```json
    {
      "ssn": "123456789",
      "fname": "John",
      "salary": 55000.0,
      "valid_from": "2026-05-01T10:00:00",
      "valid_to": "2026-05-15T14:30:00",
      "tx_time": "2026-05-01T10:00:00"
    }
    ```

### B. Fetch Preceding State (Previous Version)
Get the version of the data immediately preceding the currently active one.
*   **Request**: `GET /api/employee/{ssn}/history/previous`
*   **cURL Example**:
    ```bash
    curl http://localhost:8080/api/employee/123456789/history/previous
    ```

### C. Version Traversing (Relative to Value)
Find the state immediately *before* a specific value changes:
*   **Request**: `GET /api/employee/{ssn}/history/{filterColumn}/previous/{value}`
*   *Example*: Find employee's state before they earned `65000`:
    ```bash
    curl http://localhost:8080/api/employee/123456789/history/salary/previous/65000
    ```

Find the state immediately *after* a specific value changed:
*   **Request**: `GET /api/employee/{ssn}/history/{filterColumn}/next/{value}`
*   *Example*: Find employee's state after they earned `55000`:
    ```bash
    curl http://localhost:8080/api/employee/123456789/history/salary/next/55000
    ```

---

## 3. Advanced Temporal Queries (Complex Joins)

These endpoints run database-side overlap math to correlate multiple historical timelines.

### A. Get Employee's Manager During a Salary Period
*   **Question**: *Who was managing employee `123456789` when they had a salary of `55000`?*
*   **Request**: `GET /api/temporal/{ssn}/manager/salary/{salary}`
*   **cURL Example**:
    ```bash
    curl http://localhost:8080/api/temporal/123456789/manager/salary/55000
    ```
*   **Response**:
    ```json
    [
      {
        "ssn": "123456789",
        "super_ssn": "888665555",
        "valid_from": "2026-05-01T10:00:00",
        "valid_to": "2026-05-15T14:30:00",
        "tx_time": "2026-05-01T10:00:00"
      }
    ]
    ```

### B. Get Manager's Hours Worked on Project During Subordinate's Salary Period
*   **Question**: *How many hours did my manager work on Project `2` during the period when I had a salary of `55000`?*
*   **Request**: `GET /api/temporal/{ssn}/works-on/{pno}/salary/{salary}`
*   **cURL Example**:
    ```bash
    curl http://localhost:8080/api/temporal/123456789/works-on/2/salary/55000
    ```
*   **Response**:
    ```json
    [
      {
        "essn": "888665555",
        "pno": 2,
        "hours": 10.0,
        "valid_from": "2026-05-01T10:00:00",
        "valid_to": "2026-05-20T17:00:00"
      }
    ]
    ```
