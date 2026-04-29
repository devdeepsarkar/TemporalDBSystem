package com.temporaldb.backend.dto;

import lombok.Data;
import java.util.List;

@Data
public class TemporalRequestDTO {
    // List of columns to return in the response
    private List<String> columns;
}
