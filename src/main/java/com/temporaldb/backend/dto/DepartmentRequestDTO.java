package com.temporaldb.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import java.time.LocalDate;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DepartmentRequestDTO {
    private Integer dnumber;        // Department number — PK (include on CREATE; comes from URL on UPDATE/DELETE)
    private String  dname;          // Department name — temporal attribute (tracked in department_history)
    private String  mgr_ssn;        // Manager's SSN — temporal attribute (tracked in department_history)
    private LocalDate mgr_start_date; // Manager start date — temporal attribute (tracked in department_history)
}
