package com.recplatform.data.datasource.dto;

import com.recplatform.common.enums.DataSourceType;
import com.recplatform.data.datasource.DataSourceStatus;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Data source response VO.
 */
@Data
@Builder
public class DataSourceVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String description;
    private DataSourceType type;
    private String host;
    private Integer port;
    private String database;
    private String username;
    private String password;
    private DataSourceStatus status;
    private LocalDateTime lastTestTime;
    private String lastTestResult;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
