package com.recplatform.data.datasource.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * Connection test result DTO.
 */
@Data
@Builder
public class ConnectionTestResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean success;
    private String message;
    private long latencyMs;
    private Map<String, Object> metadata;
}
