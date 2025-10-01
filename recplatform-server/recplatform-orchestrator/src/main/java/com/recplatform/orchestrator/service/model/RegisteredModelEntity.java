package com.recplatform.orchestrator.service.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.recplatform.common.enums.AlgorithmType;
import com.recplatform.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * Entity for registered solver models.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "registered_model", autoResultMap = true)
public class RegisteredModelEntity extends BaseEntity {

    private String name;
    private String description;
    private Long solverProblemId;
    private AlgorithmType algorithmType;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> defaultConfig;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> inputSchema;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> outputSchema;

    private Integer version;
    private String status;
}
