package com.temporaldb.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorksOnRequestDTO {
    private String essn;   // Employee SSN (FK → employee.ssn)
    private String pno;    // Project number (FK → project.pnumber)
    private Double hours;  // Hours worked — temporal attribute (tracked in works_on_history)
}
