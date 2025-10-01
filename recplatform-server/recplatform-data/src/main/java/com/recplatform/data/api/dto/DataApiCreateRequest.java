package com.recplatform.data.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * Data API create/update request DTO.
 */
@Data
public class DataApiCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Name must not be blank")
    private String name;
    private String description;
    @NotBlank(message = "Path must not be blank")
    private String path;
    @NotBlank(message = "Method must not be blank")
    private String method;
    @NotNull(message = "Data source ID must not be null")
    private Long dataSourceId;
    @NotBlank(message = "Query must not be blank")
    private String query;
    private Map<String, Object> parameters;
    private Map<String, Object> responseMapping;
    private Boolean authRequired;
    private Integer rateLimit;
}
