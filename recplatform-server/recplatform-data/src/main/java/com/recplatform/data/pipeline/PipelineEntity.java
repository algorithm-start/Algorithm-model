package com.recplatform.data.pipeline;

import com.baomidou.mybatisplus.annotation.TableName;
import com.recplatform.common.enums.PipelineStatus;
import com.recplatform.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Pipeline entity.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("data_pipeline")
public class PipelineEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @com.baomidou.mybatisplus.annotation.TableField(value = "workspace_id", fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private Long workspaceId;

    private String name;
    private String description;
    private String definition;
    private PipelineMode mode;
    private String schedule;
    private PipelineStatus status;
    private LocalDateTime lastRunTime;
}
