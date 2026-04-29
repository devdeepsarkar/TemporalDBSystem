package com.temporaldb.backend.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.temporaldb.backend.dto.EmployeeRequestDTO;
import com.temporaldb.backend.service.GenericService;
import com.temporaldb.backend.service.TemporalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/employee")
public class EmployeeController {

    @Autowired
    private GenericService genericService;

    @Autowired
    private TemporalService temporalService;

    @Autowired
    private ObjectMapper objectMapper;

    private final String TABLE_NAME = "employee";

    @PostMapping
    public ResponseEntity<?> createEmployee(@RequestBody EmployeeRequestDTO dto) {
        try {
            Map<String, Object> dataMap = objectMapper.convertValue(dto, new TypeReference<Map<String, Object>>() {
            });
            Object result = genericService.processRequest(TABLE_NAME, "CREATE", dataMap, null);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{ssn}")
    public ResponseEntity<?> updateEmployee(@PathVariable String ssn, @RequestBody EmployeeRequestDTO dto) {
        try {
            Map<String, Object> dataMap = objectMapper.convertValue(dto, new TypeReference<Map<String, Object>>() {
            });

            // Build the keys map explicitly since the controller knows 'ssn' is the
            // identifier
            Map<String, Object> keysMap = new HashMap<>();
            keysMap.put("ssn", ssn);

            Object result = genericService.processRequest(TABLE_NAME, "UPDATE", dataMap, keysMap);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{ssn}")
    public ResponseEntity<?> getEmployee(@PathVariable String ssn) {
        try {
            Map<String, Object> keysMap = new HashMap<>();
            keysMap.put("ssn", ssn);

            Object result = genericService.processRequest(TABLE_NAME, "READ", null, keysMap);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllEmployees() {
        try {
            Object result = genericService.processRequest(TABLE_NAME, "READ", null, null);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{ssn}")
    public ResponseEntity<?> deleteEmployee(@PathVariable String ssn) {
        try {
            Map<String, Object> keysMap = new HashMap<>();
            keysMap.put("ssn", ssn);

            Object result = genericService.processRequest(TABLE_NAME, "DELETE", null, keysMap);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // --- TEMPORAL QUERIES ---

    // {FIRST} — first recorded state
    @GetMapping("/{ssn}/history/first")
    public ResponseEntity<?> getFirstHistory(@PathVariable String ssn) {
        try {
            Map<String, Object> keysMap = new HashMap<>();
            keysMap.put("ssn", ssn);
            Object result = temporalService.getFirstValue(TABLE_NAME, keysMap);
            return result != null ? ResponseEntity.ok(result) : ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // {LAST} — most recent recorded state
    @GetMapping("/{ssn}/history/last")
    public ResponseEntity<?> getLastHistory(@PathVariable String ssn) {
        try {
            Map<String, Object> keysMap = new HashMap<>();
            keysMap.put("ssn", ssn);
            Object result = temporalService.getLastValue(TABLE_NAME, keysMap);
            return result != null ? ResponseEntity.ok(result) : ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // PREVIOUS — state immediately before the current active state
    @GetMapping("/{ssn}/history/previous")
    public ResponseEntity<?> getPreviousHistory(@PathVariable String ssn) {
        try {
            Map<String, Object> keysMap = new HashMap<>();
            keysMap.put("ssn", ssn);
            Object result = temporalService.getPreviousValue(TABLE_NAME, keysMap);
            return result != null ? ResponseEntity.ok(result)
                    : ResponseEntity.ok(Map.of("message", "No previous record found for employee with SSN " + ssn));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // PREVIOUS 'Val' — most-recent state before the earliest occurrence of
    // filterColumn = value
    @GetMapping("/{ssn}/history/{filterColumn}/previous/{value}")
    public ResponseEntity<?> getPreviousValueHistory(
            @PathVariable String ssn,
            @PathVariable String filterColumn,
            @PathVariable String value) {
        try {
            Map<String, Object> keysMap = new HashMap<>();
            keysMap.put("ssn", ssn);
            Map<String, Object> result = temporalService.getPreviousValue(TABLE_NAME, keysMap, filterColumn, value);
            // Return 200 OK with empty body message when no prior record exists
            return result != null
                    ? ResponseEntity.ok(result)
                    : ResponseEntity
                            .ok(Map.of("message", "No previous record found before " + filterColumn + " = " + value));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // NEXT 'Val' — state after the most recent state where filterColumn = value
    @GetMapping("/{ssn}/history/{filterColumn}/next/{value}")
    public ResponseEntity<?> getNextValueHistory(
            @PathVariable String ssn,
            @PathVariable String filterColumn,
            @PathVariable String value) {
        Map<String, Object> keysMap = new HashMap<>();
        keysMap.put("ssn", ssn);
        Map<String, Object> result = temporalService.getNextValue(TABLE_NAME, keysMap, filterColumn, value);
        // Return 200 OK with descriptive message when no next record exists
        return result != null
                ? ResponseEntity.ok(result)
                : ResponseEntity.ok(Map.of("message", "No next record found after " + filterColumn + " = " + value));
    }
}
