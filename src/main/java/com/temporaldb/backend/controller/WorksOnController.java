package com.temporaldb.backend.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.temporaldb.backend.dto.WorksOnRequestDTO;
import com.temporaldb.backend.service.GenericService;
import com.temporaldb.backend.service.TemporalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/works-on")
public class WorksOnController {
    private static final String TABLE_NAME = "works_on";

    @Autowired
    private GenericService genericService;

    @Autowired
    private TemporalService temporalService;

    @Autowired
    private ObjectMapper objectMapper;

    @PostMapping
    public ResponseEntity<?> createWorksOn(@RequestBody WorksOnRequestDTO dto) {
        try {
            Map<String, Object> dataMap = objectMapper.convertValue(dto, new TypeReference<Map<String, Object>>() {
            });
            Object result = genericService.processRequest(TABLE_NAME, "CREATE", dataMap, null);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllWorksOn() {
        try {
            Object result = genericService.processRequest(TABLE_NAME, "READ", null, null);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{essn}/{pno}")
    public ResponseEntity<?> getWorksOn(
            @PathVariable String essn,
            @PathVariable String pno) {
        try {
            Map<String, Object> keysMap = buildKeys(essn, pno);
            Object result = genericService.processRequest(TABLE_NAME, "READ", null, keysMap);
            if (result == null) {
                return ResponseEntity.ok(
                        Map.of("message", "No works_on record found for essn=" + essn + ", pno=" + pno));
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{essn}/{pno}")
    public ResponseEntity<?> updateWorksOn(
            @PathVariable String essn,
            @PathVariable String pno,
            @RequestBody WorksOnRequestDTO dto) {
        try {
            Map<String, Object> dataMap = objectMapper.convertValue(dto, new TypeReference<Map<String, Object>>() {
            });

            // Remove null values so GenericService only updates the provided fields
            dataMap.entrySet().removeIf(entry -> entry.getValue() == null);

            // Composite key — GenericService uses these to identify the row
            Map<String, Object> keysMap = buildKeys(essn, pno);

            Object result = genericService.processRequest(TABLE_NAME, "UPDATE", dataMap, keysMap);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{essn}/{pno}")
    public ResponseEntity<?> deleteWorksOn(
            @PathVariable String essn,
            @PathVariable String pno) {
        try {
            Map<String, Object> keysMap = buildKeys(essn, pno);
            Object result = genericService.processRequest(TABLE_NAME, "DELETE", null, keysMap);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // TEMPORAL QUERIES — read from works_on_history

    @GetMapping("/{essn}/{pno}/history/first")
    public ResponseEntity<?> getFirstHistory(
            @PathVariable String essn,
            @PathVariable String pno) {
        try {
            Map<String, Object> keysMap = buildKeys(essn, pno);
            Object result = temporalService.getFirstValue(TABLE_NAME, keysMap);
            return result != null
                    ? ResponseEntity.ok(result)
                    : ResponseEntity.ok(Map.of("message",
                            "No history found for essn=" + essn + ", pno=" + pno));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{essn}/{pno}/history/last")
    public ResponseEntity<?> getLastHistory(
            @PathVariable String essn,
            @PathVariable String pno) {
        try {
            Map<String, Object> keysMap = buildKeys(essn, pno);
            Object result = temporalService.getLastValue(TABLE_NAME, keysMap);
            return result != null
                    ? ResponseEntity.ok(result)
                    : ResponseEntity.ok(Map.of("message",
                            "No history found for essn=" + essn + ", pno=" + pno));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{essn}/{pno}/history/previous")
    public ResponseEntity<?> getPreviousHistory(
            @PathVariable String essn,
            @PathVariable String pno) {
        try {
            Map<String, Object> keysMap = buildKeys(essn, pno);
            Object result = temporalService.getPreviousValue(TABLE_NAME, keysMap);
            return result != null
                    ? ResponseEntity.ok(result)
                    : ResponseEntity.ok(Map.of("message",
                            "No previous record found for essn=" + essn + ", pno=" + pno));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{essn}/{pno}/history/{filterColumn}/previous/{value}")
    public ResponseEntity<?> getPreviousValueHistory(
            @PathVariable String essn,
            @PathVariable String pno,
            @PathVariable String filterColumn,
            @PathVariable String value) {
        try {
            Map<String, Object> keysMap = buildKeys(essn, pno);
            Map<String, Object> result = temporalService.getPreviousValue(TABLE_NAME, keysMap, filterColumn, value);
            return result != null
                    ? ResponseEntity.ok(result)
                    : ResponseEntity.ok(Map.of("message",
                            "No previous record found before " + filterColumn + " = " + value
                                    + " for essn=" + essn + ", pno=" + pno));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{essn}/{pno}/history/{filterColumn}/next/{value}")
    public ResponseEntity<?> getNextValueHistory(
            @PathVariable String essn,
            @PathVariable String pno,
            @PathVariable String filterColumn,
            @PathVariable String value) {
        try {
            Map<String, Object> keysMap = buildKeys(essn, pno);
            Map<String, Object> result = temporalService.getNextValue(TABLE_NAME, keysMap, filterColumn, value);
            return result != null
                    ? ResponseEntity.ok(result)
                    : ResponseEntity.ok(Map.of("message",
                            "No next record found after " + filterColumn + " = " + value
                                    + " for essn=" + essn + ", pno=" + pno));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private Map<String, Object> buildKeys(String essn, String pno) {
        Map<String, Object> keys = new HashMap<>();
        keys.put("essn", essn);
        keys.put("pno", pno);
        return keys;
    }
}
