package com.recplatform.data.query.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * Ad-hoc query request DTO.
 */
@Data
public class QueryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "Data source ID must not be null")
    private Long dataSourceId;

    @NotBlank(message = "Query must not be blank")
    private String query;

    private Map<String, Object> parameters;

    private Integer limit;
}
