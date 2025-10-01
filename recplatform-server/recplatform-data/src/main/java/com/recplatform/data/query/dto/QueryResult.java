package com.recplatform.data.query.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Query result DTO.
 */
@Data
@Builder
public class QueryResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<ColumnMeta> columns;
    private List<Map<String, Object>> rows;
    private long total;
    private long executeTimeMs;
}
