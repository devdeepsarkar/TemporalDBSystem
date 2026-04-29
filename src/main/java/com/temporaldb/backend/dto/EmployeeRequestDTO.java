package com.temporaldb.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import java.time.LocalDate;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmployeeRequestDTO {
    private String ssn;
    private String fname;
    private String minit;
    private String lname;
    private LocalDate bdate;
    private String address;
    private String sex;
    private Double salary;
    private String super_ssn;
    private Integer dno;
}
