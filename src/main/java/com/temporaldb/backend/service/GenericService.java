package com.temporaldb.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;

@Service
public class GenericService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private List<String> getTableColumns(String tableName) {
        String sql = "SELECT column_name FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ?";
        return jdbcTemplate.queryForList(sql, String.class, tableName).stream()
                .map(String::toLowerCase)
                .collect(Collectors.toList());
    }

    // Checks if a table exists in the current database schema
    private boolean tableExists(String tableName) {
        String sql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tableName);
        return count != null && count > 0;
    }

    @Transactional
    public Object processRequest(String tableName, String operation, Map<String, Object> data,
            Map<String, Object> keys) {
        if (!tableExists(tableName)) {
            throw new IllegalArgumentException("Table does not exist: " + tableName);
        }

        // Determine if it's a temporal table by checking if a history table exists
        boolean isTemporal = tableExists(tableName + "_history");

        switch (operation.toUpperCase()) {
            case "CREATE":
                return createRecord(tableName, data, isTemporal);
            case "UPDATE":
                return updateRecord(tableName, data, keys, isTemporal);
            case "DELETE":
                return deleteRecord(tableName, keys, isTemporal);
            case "READ":
                return readRecord(tableName, keys);
            default:
                throw new IllegalArgumentException("Unsupported operation: " + operation);
        }
    }

    private Object createRecord(String tableName, Map<String, Object> data, boolean isTemporal) {
        if (data == null || data.isEmpty())
            throw new IllegalArgumentException("Data is required for CREATE");

        LocalDateTime now = LocalDateTime.now();

        if (isTemporal) {
            List<String> normalColumns = getTableColumns(tableName);
            // For a new record, set validity period if not provided, ONLY if columns exist
            // in normal table
            if (normalColumns.contains("valid_from") && !data.containsKey("valid_from")) {
                data.put("valid_from", now);
            }
        }

        SimpleJdbcInsert simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName(tableName);

        simpleJdbcInsert.execute(data);

        if (isTemporal) {
            String historyTable = tableName + "_history";
            SimpleJdbcInsert historyInsert = new SimpleJdbcInsert(jdbcTemplate)
                    .withTableName(historyTable);

            Map<String, Object> historyData = new HashMap<>(data);
            historyData.put("valid_from", now);
            historyData.put("valid_to", null);
            historyData.put("tx_time", now);
            historyInsert.execute(historyData);
        }

        return "Record created successfully in " + tableName;
    }

    private Object updateRecord(String tableName, Map<String, Object> data, Map<String, Object> keys,
            boolean isTemporal) {
        if (keys == null || keys.isEmpty())
            throw new IllegalArgumentException("Keys are required for UPDATE");
        if (data == null || data.isEmpty())
            throw new IllegalArgumentException("Data is required for UPDATE");

        boolean isTemporalUpdate = false;
        LocalDateTime now = LocalDateTime.now();

        if (isTemporal) {
            String historyTable = tableName + "_history";
            List<String> historyColumns = getTableColumns(historyTable);
            List<String> ignoredColumns = Arrays.asList("history_id", "valid_from", "valid_to", "tx_time");

            // Check if any of the updated data keys are in the history table
            for (String key : data.keySet()) {
                if (historyColumns.contains(key.toLowerCase()) &&
                        !ignoredColumns.contains(key.toLowerCase())) {
                    isTemporalUpdate = true;
                    break;
                }
            }
        }

        String setClause = data.keySet().stream().map(k -> k + " = ?").collect(Collectors.joining(", "));
        String whereClause = keys.keySet().stream().map(k -> k + " = ?").collect(Collectors.joining(" AND "));

        String sql = String.format("UPDATE %s SET %s WHERE %s", tableName, setClause, whereClause);

        List<Object> params = new ArrayList<>(data.values());
        params.addAll(keys.values());

        int rows = jdbcTemplate.update(sql, params.toArray());

        if (isTemporalUpdate) {
            updateHistoryRecord(tableName, keys, now);
        }

        return rows + " record(s) updated in " + tableName;
    }

    private Object deleteRecord(String tableName, Map<String, Object> keys, boolean isTemporal) {
        if (keys == null || keys.isEmpty())
            throw new IllegalArgumentException("Keys are required for DELETE");

        if (isTemporal) {
            String historyTable = tableName + "_history";
            String whereClause = keys.keySet().stream().map(k -> k + " = ?").collect(Collectors.joining(" AND "));

            // Mark the active history record as deleted by setting valid_to
            String updateHistorySql = String.format("UPDATE %s SET valid_to = ? WHERE %s AND valid_to IS NULL",
                    historyTable, whereClause);
            List<Object> updateParams = new ArrayList<>();
            updateParams.add(LocalDateTime.now());
            updateParams.addAll(keys.values());
            jdbcTemplate.update(updateHistorySql, updateParams.toArray());
        }

        String whereClause = keys.keySet().stream().map(k -> k + " = ?").collect(Collectors.joining(" AND "));
        String sql = String.format("DELETE FROM %s WHERE %s", tableName, whereClause);

        int rows = jdbcTemplate.update(sql, keys.values().toArray());
        return rows + " record(s) deleted from " + tableName;
    }

    private Object readRecord(String tableName, Map<String, Object> keys) {
        if (keys == null || keys.isEmpty()) {
            String sql = String.format("SELECT * FROM %s", tableName);
            return jdbcTemplate.queryForList(sql);
        }

        String whereClause = keys.keySet().stream().map(k -> k + " = ?").collect(Collectors.joining(" AND "));
        String sql = String.format("SELECT * FROM %s WHERE %s", tableName, whereClause);
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, keys.values().toArray());

        if (results.isEmpty()) {
            return null;
        }

        return results.get(0);
    }

    private void updateHistoryRecord(String tableName, Map<String, Object> keys, LocalDateTime now) {
        String historyTable = tableName + "_history";
        String whereClause = keys.keySet().stream().map(k -> k + " = ?").collect(Collectors.joining(" AND "));

        // 1. Mark the current active history record as historical
        String updateHistorySql = String.format("UPDATE %s SET valid_to = ? WHERE %s AND valid_to IS NULL",
                historyTable, whereClause);
        List<Object> updateParams = new ArrayList<>();
        updateParams.add(now);
        updateParams.addAll(keys.values());
        jdbcTemplate.update(updateHistorySql, updateParams.toArray());

        // 2. Fetch the newly updated record from the normal table
        String selectSql = String.format("SELECT * FROM %s WHERE %s", tableName, whereClause);
        List<Map<String, Object>> newRecords = jdbcTemplate.queryForList(selectSql, keys.values().toArray());

        if (newRecords.isEmpty()) {
            return;
        }

        SimpleJdbcInsert historyInsert = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName(historyTable);

        // 3. Insert the new state into the history table
        for (Map<String, Object> record : newRecords) {
            record.put("valid_from", now);
            record.put("valid_to", null); // Active record
            record.put("tx_time", now);
            historyInsert.execute(record);
        }
    }
}
