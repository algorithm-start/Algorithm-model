package com.recplatform.orchestrator.flow;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.recplatform.common.enums.FlowStatus;
import com.recplatform.common.model.BaseEntity;
import com.recplatform.orchestrator.flow.dto.FlowDefinition;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * Orchestration flow entity.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "orchestration_flow", autoResultMap = true)
public class FlowEntity extends BaseEntity {

    private String name;

    private String description;

    private Integer version;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private FlowDefinition definition;

    private FlowStatus status;

    private String category;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> tags;
}
