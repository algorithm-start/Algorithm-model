package com.recplatform.orchestrator.flow;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.recplatform.common.enums.FlowStatus;
import com.recplatform.orchestrator.flow.vo.NodeExecutionStatus;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Flow execution record entity.
 */
@Data
@TableName(value = "flow_execution", autoResultMap = true)
public class FlowExecutionEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    private Long id;

    private Long flowId;

    private Integer flowVersion;

    private FlowStatus status;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> inputParams;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> outputResult;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<NodeExecutionStatus> nodeExecutions;

    private String errorMessage;

    /**
     * Workspace this execution belongs to, inherited from its flow. Used by the
     * WorkspaceInterceptor to isolate execution records across workspaces.
     */
    private Long workspaceId;

    private String createBy;
}
