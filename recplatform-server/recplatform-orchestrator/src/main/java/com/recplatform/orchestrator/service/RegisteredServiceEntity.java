package com.recplatform.orchestrator.service;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.recplatform.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Entity for registered external services.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "registered_service", autoResultMap = true)
public class RegisteredServiceEntity extends BaseEntity {

    private String name;
    private String description;
    private String type;
    private String baseUrl;
    private String healthCheckPath;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> authConfig;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> inputSchema;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> outputSchema;

    private String status;
    private LocalDateTime lastHealthCheck;
}
