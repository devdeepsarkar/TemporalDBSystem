package com.temporaldb.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TemporalService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Normalize all DB column keys to lowercase for consistent response
    private Map<String, Object> normalizeRow(Map<String, Object> row) {
        if (row == null)
            return null;
        Map<String, Object> normalized = new HashMap<>();
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            normalized.put(entry.getKey().toLowerCase(), entry.getValue());
        }
        return normalized;
    }

    private String buildWhereClause(Map<String, Object> keys) {
        return keys.keySet().stream().map(k -> k + " = ?").collect(Collectors.joining(" AND "));
    }

    // {FIRST} Column — first recorded state for the given keys
    public Map<String, Object> getFirstValue(String tableName, Map<String, Object> keys) {
        String historyTable = tableName + "_history";
        String whereClause = buildWhereClause(keys);
        // MIN(valid_from) is equivalent to ORDER BY valid_from ASC LIMIT 1
        String sql = String.format(
                "SELECT * FROM %s WHERE %s AND valid_from = (SELECT MIN(valid_from) FROM %s WHERE %s)",
                historyTable, whereClause, historyTable, whereClause);
        Object[] params = new Object[keys.size() * 2];
        int i = 0;
        for (Object v : keys.values()) params[i++] = v;
        for (Object v : keys.values()) params[i++] = v;
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, params);
        return results.isEmpty() ? null : normalizeRow(results.get(0));
    }

    // {LAST} Column — most recent recorded state for the given keys
    public Map<String, Object> getLastValue(String tableName, Map<String, Object> keys) {
        String historyTable = tableName + "_history";
        String whereClause = buildWhereClause(keys);
        // MAX(valid_from) is equivalent to ORDER BY valid_from DESC LIMIT 1
        String sql = String.format(
                "SELECT * FROM %s WHERE %s AND valid_from = (SELECT MAX(valid_from) FROM %s WHERE %s)",
                historyTable, whereClause, historyTable, whereClause);
        Object[] params = new Object[keys.size() * 2];
        int i = 0;
        for (Object v : keys.values()) params[i++] = v;
        for (Object v : keys.values()) params[i++] = v;
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, params);
        return results.isEmpty() ? null : normalizeRow(results.get(0));
    }

    // PREVIOUS — state immediately before the current active state
    public Map<String, Object> getPreviousValue(String tableName, Map<String, Object> keys) {
        String historyTable = tableName + "_history";
        String whereClause = buildWhereClause(keys);
        // Equivalent to ORDER BY valid_from DESC LIMIT 1 OFFSET 1:
        // find the max valid_from that is strictly less than the overall max valid_from
        String sql = String.format(
                "SELECT * FROM %s WHERE %s" +
                "  AND valid_from = (" +
                "    SELECT MAX(valid_from) FROM %s WHERE %s" +
                "      AND valid_from < (SELECT MAX(valid_from) FROM %s WHERE %s)" +
                "  )",
                historyTable, whereClause,
                historyTable, whereClause,
                historyTable, whereClause);
        Object[] params = new Object[keys.size() * 3];
        int i = 0;
        for (Object v : keys.values()) params[i++] = v; // outer WHERE
        for (Object v : keys.values()) params[i++] = v; // middle MAX WHERE
        for (Object v : keys.values()) params[i++] = v; // inner MAX WHERE
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, params);
        return results.isEmpty() ? null : normalizeRow(results.get(0));
    }

    // PREVIOUS 'Val' — the single most-recent state that started before the
    // EARLIEST state where filterColumn = value (the given value is excluded)
    public Map<String, Object> getPreviousValue(String tableName, Map<String, Object> keys, String filterColumn,
            Object value) {
        String historyTable = tableName + "_history";
        String keysWhere = buildWhereClause(keys);

        // MIN(valid_from) anchors on the earliest occurrence of filterColumn = value
        // (equivalent to ORDER BY valid_from ASC LIMIT 1 on the sub-query)
        String subQuery = String.format(
                "SELECT MIN(valid_from) FROM %s WHERE %s AND %s = ?",
                historyTable, keysWhere, filterColumn);

        // MAX(valid_from) where valid_from < sub picks the row immediately before
        // (equivalent to ORDER BY valid_from DESC LIMIT 1 on the outer query)
        String sql = String.format(
                "SELECT * FROM %s WHERE %s AND valid_from = (SELECT MAX(valid_from) FROM %s WHERE %s AND valid_from < (%s))",
                historyTable, keysWhere, historyTable, keysWhere, subQuery);

        // params: outer WHERE keys + inner MAX WHERE keys + sub-query keys + value
        Object[] params = new Object[keys.size() * 3 + 1];
        int i = 0;
        for (Object v : keys.values()) params[i++] = v; // outer WHERE
        for (Object v : keys.values()) params[i++] = v; // inner MAX WHERE
        for (Object v : keys.values()) params[i++] = v; // sub-query WHERE
        params[i] = value;

        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, params);
        return results.isEmpty() ? null : normalizeRow(results.get(0));
    }

    // NEXT 'Val' — state that started just after the most recent state where
    // filterColumn = value
    public Map<String, Object> getNextValue(String tableName, Map<String, Object> keys, String filterColumn,
            Object value) {
        String historyTable = tableName + "_history";
        String keysWhere = buildWhereClause(keys);

        // MAX(valid_from) anchors on the most recent occurrence of filterColumn = value
        // (equivalent to ORDER BY valid_from DESC LIMIT 1 on the sub-query)
        String subQuery = String.format(
                "SELECT MAX(valid_from) FROM %s WHERE %s AND %s = ?",
                historyTable, keysWhere, filterColumn);

        // MIN(valid_from) where valid_from > sub picks the row immediately after
        // (equivalent to ORDER BY valid_from ASC LIMIT 1 on the outer query)
        String sql = String.format(
                "SELECT * FROM %s WHERE %s AND valid_from = (SELECT MIN(valid_from) FROM %s WHERE %s AND valid_from > (%s))",
                historyTable, keysWhere, historyTable, keysWhere, subQuery);

        // params: outer WHERE keys + inner MIN WHERE keys + sub-query keys + value
        Object[] params = new Object[keys.size() * 3 + 1];
        int i = 0;
        for (Object v : keys.values()) params[i++] = v; // outer WHERE
        for (Object v : keys.values()) params[i++] = v; // inner MIN WHERE
        for (Object v : keys.values()) params[i++] = v; // sub-query WHERE
        params[i] = value;

        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, params);
        return results.isEmpty() ? null : normalizeRow(results.get(0));
    }

    // Dynamic When(target_attr, target_where, when_condition)
    // Returns all columns of target rows that temporally overlap with rows
    // satisfying when_condition.
    //
    // Diagram interpretation:
    //   when(employee, super_ssn, "ssn = 10124", "salary = 100000 AND ssn = 10126")
    //   -> target rows: employee_history WHERE ssn = 10124
    //   -> condition rows: employee_history WHERE salary = 100000 AND ssn = 10126
    //   -> JOIN on overlapping [valid_from, valid_to]
    //   -> return target.* (including target_attr = super_ssn)
    public List<Map<String, Object>> executeWhen(
            String tableName,       // e.g. "employee"
            String targetAttr,      // e.g. "super_ssn" (informational; full row returned)
            String targetWhere,     // SQL WHERE fragment for target rows, e.g. "ssn = '10124'"
            String whenCondition) { // SQL WHERE fragment for condition rows, e.g. "salary = 100000 AND ssn = '10126'"

        String historyTable = tableName + "_history";

        // Standard temporal join using strict overlap: start1 < end2 AND start2 < end1
        // (null valid_to = open-ended / current, treated as 9999-12-31 23:59:59)
        String sql = String.format(
                "SELECT DISTINCT target.* " +
                "FROM %s target " +
                "JOIN ( " +
                "    SELECT valid_from, valid_to " +
                "    FROM %s " +
                "    WHERE %s " +
                ") cond " +
                "ON target.valid_from < IFNULL(cond.valid_to, '9999-12-31 23:59:59') " +
                "   AND cond.valid_from < IFNULL(target.valid_to, '9999-12-31 23:59:59') " +
                "WHERE %s",
                historyTable,
                historyTable,
                whenCondition,
                targetWhere);

        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
        return results.stream().map(this::normalizeRow).collect(Collectors.toList());
    }

    // Standard Temporal Join — joins two history tables on overlapping
    // [valid_from, valid_to] intervals and returns all matching rows.
    //
    // Use case example:
    //   "Hours worked on project pno by employee ssn WHEN salary was 100000"
    //   -> targetTable = "works_on",  targetWhere = "ssn = '123'"
    //   -> condTable   = "employee",  condWhere   = "ssn = '123' AND salary = 100000"
    //   -> result: works_on rows whose validity overlaps with the salary-100000 period
    //
    // Overlap condition (strict, matching IntervalUtility.overlap()):
    //   target.valid_from < IFNULL(cond.valid_to, INF)
    //   AND cond.valid_from < IFNULL(target.valid_to, INF)
    public List<Map<String, Object>> executeTimeSlice(
            String targetTable,  // table whose rows we want, e.g. "works_on"
            String targetWhere,  // SQL WHERE for target, e.g. "ssn = '123'"
            String condTable,    // table providing the time window, e.g. "employee"
            String condWhere) {  // SQL WHERE for condition, e.g. "ssn = '123' AND salary = 100000"

        String targetHistory = targetTable + "_history";
        String condHistory   = condTable   + "_history";

        // Return all target columns; prefix them to avoid ambiguity with cond columns
        String sql = String.format(
                "SELECT DISTINCT t.* " +
                "FROM %s t " +
                "JOIN ( " +
                "    SELECT valid_from, valid_to " +
                "    FROM %s " +
                "    WHERE %s " +
                ") c " +
                "ON t.valid_from < IFNULL(c.valid_to, '9999-12-31 23:59:59') " +
                "   AND c.valid_from < IFNULL(t.valid_to, '9999-12-31 23:59:59') " +
                "WHERE %s",
                targetHistory,
                condHistory,
                condWhere,
                targetWhere);

        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
        return results.stream().map(this::normalizeRow).collect(Collectors.toList());
    }
}
