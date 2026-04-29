package com.temporaldb.backend.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.temporaldb.backend.dto.DepartmentRequestDTO;
import com.temporaldb.backend.service.GenericService;
import com.temporaldb.backend.service.TemporalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for CRUD operations on the department table.
 *
 * The department table has a single primary key: dnumber (department number).
 * The columns dname, mgr_ssn, and mgr_start_date are temporal attributes —
 * any update to them is automatically reflected in department_history via GenericService.
 *
 * Base URL: /api/department
 *
 * Endpoints:
 *   POST   /api/department                              → Create a department record
 *   GET    /api/department                              → Get all department records
 *   GET    /api/department/{dnumber}                    → Get a specific department by PK
 *   PUT    /api/department/{dnumber}                    → Update a department (temporal attrs → writes to history)
 *   DELETE /api/department/{dnumber}                    → Delete a department (closes active history entry)
 *
 * Temporal history endpoints (reads from department_history):
 *   GET    /api/department/{dnumber}/history/first
 *   GET    /api/department/{dnumber}/history/last
 *   GET    /api/department/{dnumber}/history/previous
 *   GET    /api/department/{dnumber}/history/{filterColumn}/previous/{value}
 *   GET    /api/department/{dnumber}/history/{filterColumn}/next/{value}
 */
@RestController
@RequestMapping("/api/department")
public class DepartmentController {

    /** Exact table name in the database. */
    private static final String TABLE_NAME = "department";

    @Autowired
    private GenericService genericService;

    @Autowired
    private TemporalService temporalService;

    @Autowired
    private ObjectMapper objectMapper;

    // -------------------------------------------------------------------------
    // CREATE — POST /api/department
    // -------------------------------------------------------------------------
    /**
     * Creates a new department record.
     * GenericService also inserts a row in department_history with valid_from = NOW.
     *
     * Request body example:
     * {
     *   "dnumber": 6,
     *   "dname": "IT",
     *   "mgr_ssn": "123456789",
     *   "mgr_start_date": "2024-01-15"
     * }
     */
    @PostMapping
    public ResponseEntity<?> createDepartment(@RequestBody DepartmentRequestDTO dto) {
        try {
            Map<String, Object> dataMap = objectMapper.convertValue(dto, new TypeReference<Map<String, Object>>() {});
            Object result = genericService.processRequest(TABLE_NAME, "CREATE", dataMap, null);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // READ ALL — GET /api/department
    // -------------------------------------------------------------------------
    /**
     * Returns all records from the department table (current state only, not history).
     */
    @GetMapping
    public ResponseEntity<?> getAllDepartments() {
        try {
            Object result = genericService.processRequest(TABLE_NAME, "READ", null, null);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // READ ONE — GET /api/department/{dnumber}
    // -------------------------------------------------------------------------
    /**
     * Returns the current state of the department with the given dnumber.
     *
     * @param dnumber Department number (PK)
     */
    @GetMapping("/{dnumber}")
    public ResponseEntity<?> getDepartment(@PathVariable Integer dnumber) {
        try {
            Map<String, Object> keysMap = buildKeys(dnumber);
            Object result = genericService.processRequest(TABLE_NAME, "READ", null, keysMap);
            if (result == null) {
                return ResponseEntity.ok(
                        Map.of("message", "No department found with dnumber=" + dnumber));
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // UPDATE — PUT /api/department/{dnumber}
    // -------------------------------------------------------------------------
    /**
     * Updates a department identified by dnumber.
     *
     * Because dname, mgr_ssn, and mgr_start_date are temporal attributes tracked
     * in department_history, GenericService will automatically:
     *   1. Set valid_to = NOW on the currently-active history row.
     *   2. Insert a new history row with the updated values and valid_from = NOW.
     *
     * Only send the fields you want to change in the request body (dnumber comes from the URL).
     *
     * Request body example (changing manager):
     * {
     *   "mgr_ssn": "987654321",
     *   "mgr_start_date": "2025-06-01"
     * }
     */
    @PutMapping("/{dnumber}")
    public ResponseEntity<?> updateDepartment(
            @PathVariable Integer dnumber,
            @RequestBody DepartmentRequestDTO dto) {
        try {
            Map<String, Object> dataMap = objectMapper.convertValue(dto, new TypeReference<Map<String, Object>>() {});

            // Strip null values so GenericService only updates the supplied fields
            dataMap.entrySet().removeIf(entry -> entry.getValue() == null);

            // PK used by GenericService to locate the row
            Map<String, Object> keysMap = buildKeys(dnumber);

            Object result = genericService.processRequest(TABLE_NAME, "UPDATE", dataMap, keysMap);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // DELETE — DELETE /api/department/{dnumber}
    // -------------------------------------------------------------------------
    /**
     * Deletes the department with the given dnumber.
     *
     * For temporal tables, GenericService also closes the active history entry
     * by setting valid_to = NOW before deleting the main row.
     *
     * @param dnumber Department number (PK)
     */
    @DeleteMapping("/{dnumber}")
    public ResponseEntity<?> deleteDepartment(@PathVariable Integer dnumber) {
        try {
            Map<String, Object> keysMap = buildKeys(dnumber);
            Object result = genericService.processRequest(TABLE_NAME, "DELETE", null, keysMap);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // TEMPORAL QUERIES — reads from department_history
    // -------------------------------------------------------------------------

    /**
     * Returns the FIRST recorded state (earliest valid_from) for the given department.
     *
     * GET /api/department/{dnumber}/history/first
     */
    @GetMapping("/{dnumber}/history/first")
    public ResponseEntity<?> getFirstHistory(@PathVariable Integer dnumber) {
        try {
            Map<String, Object> keysMap = buildKeys(dnumber);
            Object result = temporalService.getFirstValue(TABLE_NAME, keysMap);
            return result != null
                    ? ResponseEntity.ok(result)
                    : ResponseEntity.ok(Map.of("message",
                            "No history found for department dnumber=" + dnumber));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Returns the LAST recorded state (most recent, i.e. active or latest closed) for the given department.
     *
     * GET /api/department/{dnumber}/history/last
     */
    @GetMapping("/{dnumber}/history/last")
    public ResponseEntity<?> getLastHistory(@PathVariable Integer dnumber) {
        try {
            Map<String, Object> keysMap = buildKeys(dnumber);
            Object result = temporalService.getLastValue(TABLE_NAME, keysMap);
            return result != null
                    ? ResponseEntity.ok(result)
                    : ResponseEntity.ok(Map.of("message",
                            "No history found for department dnumber=" + dnumber));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Returns the PREVIOUS state (the one immediately before the current active state).
     *
     * GET /api/department/{dnumber}/history/previous
     */
    @GetMapping("/{dnumber}/history/previous")
    public ResponseEntity<?> getPreviousHistory(@PathVariable Integer dnumber) {
        try {
            Map<String, Object> keysMap = buildKeys(dnumber);
            Object result = temporalService.getPreviousValue(TABLE_NAME, keysMap);
            return result != null
                    ? ResponseEntity.ok(result)
                    : ResponseEntity.ok(Map.of("message",
                            "No previous record found for department dnumber=" + dnumber));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Returns the state immediately BEFORE the first occurrence of filterColumn = value.
     *
     * Example: GET /api/department/5/history/mgr_ssn/previous/987654321
     * → Returns the department_history row just before mgr_ssn became 987654321
     *
     * GET /api/department/{dnumber}/history/{filterColumn}/previous/{value}
     */
    @GetMapping("/{dnumber}/history/{filterColumn}/previous/{value}")
    public ResponseEntity<?> getPreviousValueHistory(
            @PathVariable Integer dnumber,
            @PathVariable String filterColumn,
            @PathVariable String value) {
        try {
            Map<String, Object> keysMap = buildKeys(dnumber);
            Map<String, Object> result = temporalService.getPreviousValue(TABLE_NAME, keysMap, filterColumn, value);
            return result != null
                    ? ResponseEntity.ok(result)
                    : ResponseEntity.ok(Map.of("message",
                            "No previous record found before " + filterColumn + " = " + value
                                    + " for department dnumber=" + dnumber));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Returns the state immediately AFTER the last occurrence of filterColumn = value.
     *
     * Example: GET /api/department/5/history/mgr_ssn/next/333445555
     * → Returns the department_history row just after mgr_ssn was last 333445555
     *
     * GET /api/department/{dnumber}/history/{filterColumn}/next/{value}
     */
    @GetMapping("/{dnumber}/history/{filterColumn}/next/{value}")
    public ResponseEntity<?> getNextValueHistory(
            @PathVariable Integer dnumber,
            @PathVariable String filterColumn,
            @PathVariable String value) {
        try {
            Map<String, Object> keysMap = buildKeys(dnumber);
            Map<String, Object> result = temporalService.getNextValue(TABLE_NAME, keysMap, filterColumn, value);
            return result != null
                    ? ResponseEntity.ok(result)
                    : ResponseEntity.ok(Map.of("message",
                            "No next record found after " + filterColumn + " = " + value
                                    + " for department dnumber=" + dnumber));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    /**
     * Builds the key map used by GenericService to identify a department row.
     * department has a single PK: dnumber.
     *
     * @param dnumber Department number
     * @return Map containing the PK column
     */
    private Map<String, Object> buildKeys(Integer dnumber) {
        Map<String, Object> keys = new HashMap<>();
        keys.put("dnumber", dnumber);
        return keys;
    }
}
