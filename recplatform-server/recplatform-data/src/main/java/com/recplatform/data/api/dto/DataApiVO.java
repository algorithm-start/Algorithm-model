package com.recplatform.data.api.dto;

import com.recplatform.data.api.DataApiStatus;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Data API response VO.
 */
@Data
@Builder
public class DataApiVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String description;
    private String path;
    private String method;
    private Long dataSourceId;
    private String query;
    private Map<String, Object> parameters;
    private Map<String, Object> responseMapping;
    private DataApiStatus status;
    private Boolean authRequired;
    private Integer rateLimit;
    private Long callCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
