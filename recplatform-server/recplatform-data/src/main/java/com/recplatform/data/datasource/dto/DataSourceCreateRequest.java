package com.recplatform.data.datasource.dto;

import com.recplatform.common.enums.DataSourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * Data source create request DTO.
 */
@Data
public class DataSourceCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Name must not be blank")
    private String name;

    private String description;

    @NotNull(message = "Type must not be null")
    private DataSourceType type;

    @NotNull(message = "Connection config must not be null")
    private Map<String, Object> connectionConfig;
}
