package com.recplatform.data.pipeline;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Pipeline execution entity.
 */
@Data
@TableName("pipeline_execution")
public class PipelineExecutionEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long pipelineId;
    private ExecutionStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String nodeStatuses;
    private String errorMessage;
    private String metrics;

    /**
     * Workspace this execution belongs to, inherited from its pipeline. Used by
     * the WorkspaceInterceptor to isolate execution records across workspaces.
     */
    private Long workspaceId;
}
