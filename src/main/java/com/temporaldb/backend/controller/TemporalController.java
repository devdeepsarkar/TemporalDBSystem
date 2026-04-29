package com.temporaldb.backend.controller;

import com.temporaldb.backend.service.TemporalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/temporal")
public class TemporalController {

    @Autowired
    private TemporalService temporalService;

    /**
     * API 1 — Hours spent on a given project by an employee's manager, during the
     * period
     * when that employee had a specific salary.
     *
     * Flow:
     * Step 1 → executeWhen("employee", "super_ssn", "ssn='<ssn>'", "salary=<sal>
     * AND ssn='<ssn>'")
     * Returns employee_history rows for <ssn> during the salary=<sal> period,
     * each row containing the super_ssn (manager) active at that time.
     *
     * Step 2 → Extract distinct super_ssn value(s) from Step 1.
     *
     * Step 3 → executeTimeSlice("works_on", "essn IN (...) AND pno=<pno>",
     * "employee", "salary=<sal> AND ssn='<ssn>'")
     * Returns works_on_history rows where the manager(s) worked on <pno>,
     * temporally overlapping with the salary period.
     *
     * GET /api/temporal/{ssn}/works-on/{pno}/salary/{salary}
     */
    @GetMapping("/{ssn}/works-on/{pno}/salary/{salary}")
    public ResponseEntity<?> getManagerWorksOnDuringSalary(
            @PathVariable String ssn,
            @PathVariable String pno,
            @PathVariable String salary) {
        try {
            // Step 1: Find manager (super_ssn) during the salary period
            List<Map<String, Object>> managerRows = temporalService.executeWhen(
                    "employee",
                    "super_ssn",
                    "ssn = '" + ssn + "'",
                    "salary = " + salary + " AND ssn = '" + ssn + "'");

            if (managerRows.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "message", "No employee record found for ssn=" + ssn + " with salary=" + salary));
            }

            // Step 2: Collect ALL super_ssn values (preserving duplicates across rows —
            // same manager can appear in separate intervals and all must be included)
            List<String> managerSsns = managerRows.stream()
                    .map(r -> r.get("super_ssn"))
                    .filter(v -> v != null)
                    .map(Object::toString)
                    .collect(Collectors.toList());

            if (managerSsns.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "message",
                        "Employee ssn=" + ssn + " had no manager recorded during salary=" + salary + " period"));
            }

            // Step 3: Build IN clause, then temporal-join works_on with employee's salary period
            String inClause = managerSsns.stream()
                    .map(s -> "'" + s + "'")
                    .collect(Collectors.joining(", "));

            List<Map<String, Object>> result = temporalService.executeTimeSlice(
                    "works_on",
                    "essn IN (" + inClause + ") AND pno = '" + pno + "'",
                    "employee",
                    "salary = " + salary + " AND ssn = '" + ssn + "'");

            return result != null
                    ? ResponseEntity.ok(result)
                    : ResponseEntity
                            .ok(Map.of("message", "No works_on entries found for manager(s) " + managerSsns
                                    + " on pno=" + pno + " during salary=" + salary + " period"));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * API 2 — Who was the manager of a given employee when that employee had a
     * specific salary.
     *
     * Flow:
     * executeWhen("employee", "super_ssn", "ssn='<ssn>'", "salary=<sal> AND
     * ssn='<ssn>'")
     * Returns employee_history rows for <ssn> during the salary=<sal> period,
     * each row containing the super_ssn (manager's SSN) active at that time.
     *
     * GET /api/temporal/{ssn}/manager/salary/{salary}
     */
    @GetMapping("/{ssn}/manager/salary/{salary}")
    public ResponseEntity<?> getManagerDuringSalary(
            @PathVariable String ssn,
            @PathVariable String salary) {
        try {
            List<Map<String, Object>> result = temporalService.executeWhen(
                    "employee",
                    "super_ssn",
                    "ssn = '" + ssn + "'",
                    "salary = " + salary + " AND ssn = '" + ssn + "'");

            return result != null
                    ? ResponseEntity.ok(result)
                    : ResponseEntity
                            .ok(Map.of("message", "No manager record found for ssn=" + ssn + " with salary=" + salary));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
