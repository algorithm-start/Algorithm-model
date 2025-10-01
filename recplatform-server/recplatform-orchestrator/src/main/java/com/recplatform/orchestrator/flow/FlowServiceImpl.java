package com.recplatform.orchestrator.flow;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.recplatform.common.enums.FlowStatus;
import com.recplatform.common.exception.BusinessException;
import com.recplatform.common.model.PageRequest;
import com.recplatform.common.model.PageResult;
import com.recplatform.common.result.ResultCode;
import com.recplatform.orchestrator.engine.DagValidator;
import com.recplatform.orchestrator.engine.FlowEngine;
import com.recplatform.orchestrator.engine.FlowExecutionResult;
import com.recplatform.orchestrator.flow.dto.FlowDefinition;
import com.recplatform.orchestrator.flow.mapper.FlowExecutionMapper;
import com.recplatform.orchestrator.flow.mapper.FlowMapper;
import com.recplatform.orchestrator.flow.vo.FlowExecutionVO;
import com.recplatform.orchestrator.flow.vo.FlowVO;
import com.recplatform.orchestrator.flow.vo.NodeExecutionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Implementation of FlowService.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlowServiceImpl implements FlowService {

    private final FlowMapper flowMapper;
    private final FlowExecutionMapper executionMapper;
    private final DagValidator dagValidator;
    private final FlowEngine flowEngine;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FlowVO createFlow(String name, String description, FlowDefinition definition,
                             String category, List<String> tags) {
        log.info("Creating flow: name={}", name);
        FlowEntity entity = new FlowEntity();
        entity.setName(name);
        entity.setDescription(description);
        entity.setVersion(1);
        entity.setDefinition(definition);
        entity.setStatus(FlowStatus.DRAFT);
        entity.setCategory(category);
        entity.setTags(tags != null ? tags : Collections.emptyList());
        flowMapper.insert(entity);
        return toFlowVO(entity);
    }

    @Override
    public PageResult<FlowVO> listFlows(PageRequest pageRequest, String category, String status) {
        log.info("Listing flows: page={}, size={}, category={}, status={}",
                pageRequest.getPage(), pageRequest.getSize(), category, status);
        Page<FlowEntity> page = new Page<>(pageRequest.getPage(), pageRequest.getSize());
        LambdaQueryWrapper<FlowEntity> wrapper = new LambdaQueryWrapper<>();
        if (category != null && !category.isEmpty()) {
            wrapper.eq(FlowEntity::getCategory, category);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(FlowEntity::getStatus, FlowStatus.valueOf(status));
        }
        wrapper.orderByDesc(FlowEntity::getCreateTime);
        IPage<FlowEntity> result = flowMapper.selectPage(page, wrapper);
        return PageResult.of(result.convert(this::toFlowVO));
    }

    @Override
    public FlowVO getFlow(Long id) {
        FlowEntity entity = getFlowEntityOrThrow(id);
        return toFlowVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FlowVO updateFlow(Long id, String name, String description, FlowDefinition definition,
                             String category, List<String> tags) {
        log.info("Updating flow: id={}", id);
        FlowEntity entity = getFlowEntityOrThrow(id);
        if (entity.getStatus() == FlowStatus.RUNNING) {
            throw new BusinessException(ResultCode.FLOW_INVALID_CONFIG,
                    "Cannot update a RUNNING flow");
        }
        // Saving always resets to DRAFT so it can be re-published
        entity.setStatus(FlowStatus.DRAFT);
        if (name != null) {
            entity.setName(name);
        }
        if (description != null) {
            entity.setDescription(description);
        }
        if (definition != null) {
            entity.setDefinition(definition);
        }
        if (category != null) {
            entity.setCategory(category);
        }
        if (tags != null) {
            entity.setTags(tags);
        }
        flowMapper.updateById(entity);
        return toFlowVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFlow(Long id) {
        log.info("Deleting flow: id={}", id);
        FlowEntity entity = getFlowEntityOrThrow(id);
        if (entity.getStatus() == FlowStatus.RUNNING) {
            throw new BusinessException(ResultCode.FLOW_EXECUTION_FAILED,
                    "工作流正在「执行中」，无法删除，请等待执行结束或先取消执行。");
        }
        flowMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FlowVO publishFlow(Long id) {
        log.info("Publishing flow: id={}", id);
        FlowEntity entity = getFlowEntityOrThrow(id);
        if (entity.getStatus() != FlowStatus.DRAFT) {
            throw new BusinessException(ResultCode.FLOW_INVALID_CONFIG,
                    "只有「草稿」状态的工作流才能发布。当前状态：" + statusLabel(entity.getStatus()));
        }
        // Validate DAG before publishing
        var validationResult = dagValidator.validate(entity.getDefinition());
        if (!validationResult.isValid()) {
            throw new BusinessException(ResultCode.FLOW_CYCLE_DETECTED,
                    "流程结构校验未通过：" + String.join("；", validationResult.getErrors()));
        }
        entity.setStatus(FlowStatus.PUBLISHED);
        flowMapper.updateById(entity);
        return toFlowVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FlowExecutionVO executeFlow(Long id, Map<String, Object> inputParams) {
        log.info("Executing flow: id={}", id);
        FlowEntity entity = getFlowEntityOrThrow(id);
        if (entity.getStatus() != FlowStatus.PUBLISHED) {
            throw new BusinessException(ResultCode.FLOW_EXECUTION_FAILED,
                    "只有「已发布」状态的工作流才能执行，请先发布该工作流。当前状态：" + statusLabel(entity.getStatus()));
        }

        // Create execution record
        FlowExecutionEntity execution = new FlowExecutionEntity();
        execution.setFlowId(id);
        execution.setFlowVersion(entity.getVersion());
        execution.setStatus(FlowStatus.RUNNING);
        execution.setInputParams(inputParams);
        execution.setStartTime(LocalDateTime.now());
        execution.setNodeExecutions(Collections.emptyList());
        executionMapper.insert(execution);

        // Update flow status to RUNNING
        entity.setStatus(FlowStatus.RUNNING);
        flowMapper.updateById(entity);

        // Execute the flow asynchronously
        try {
            FlowExecutionResult result = flowEngine.execute(entity.getDefinition(), inputParams, execution.getId());

            // Update execution record with results
            execution.setStatus(result.isSuccess() ? FlowStatus.COMPLETED : FlowStatus.FAILED);
            execution.setEndTime(LocalDateTime.now());
            execution.setOutputResult(result.getOutput());
            execution.setNodeExecutions(result.getNodeExecutions());
            execution.setErrorMessage(result.getErrorMessage());
            executionMapper.updateById(execution);
        } catch (Exception e) {
            log.error("Flow execution failed: flowId={}, executionId={}", id, execution.getId(), e);
            execution.setStatus(FlowStatus.FAILED);
            execution.setEndTime(LocalDateTime.now());
            execution.setErrorMessage(e.getMessage());
            executionMapper.updateById(execution);
        } finally {
            // Restore flow status to PUBLISHED
            entity.setStatus(FlowStatus.PUBLISHED);
            flowMapper.updateById(entity);
        }

        return toExecutionVO(execution);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void stopFlow(Long executionId) {
        log.info("Stopping flow execution: executionId={}", executionId);
        FlowExecutionEntity execution = executionMapper.selectById(executionId);
        if (execution == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "未找到执行记录：" + executionId);
        }
        if (execution.getStatus() != FlowStatus.RUNNING) {
            throw new BusinessException(ResultCode.FLOW_EXECUTION_FAILED,
                    "只有「执行中」的记录才能取消。当前状态：" + statusLabel(execution.getStatus()));
        }
        // Signal the engine to stop
        flowEngine.cancel(executionId);

        execution.setStatus(FlowStatus.CANCELLED);
        execution.setEndTime(LocalDateTime.now());
        executionMapper.updateById(execution);
    }

    @Override
    public FlowExecutionVO getExecution(Long executionId) {
        FlowExecutionEntity execution = executionMapper.selectById(executionId);
        if (execution == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "未找到执行记录：" + executionId);
        }
        return toExecutionVO(execution);
    }

    /**
     * Returns a Chinese label for a flow/execution status for user-facing messages.
     */
    private String statusLabel(FlowStatus status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case DRAFT:
                return "草稿";
            case PUBLISHED:
                return "已发布";
            case RUNNING:
                return "执行中";
            case PAUSED:
                return "已暂停";
            case COMPLETED:
                return "已完成";
            case FAILED:
                return "失败";
            case CANCELLED:
                return "已取消";
            default:
                return status.name();
        }
    }

    @Override
    public PageResult<FlowExecutionVO> listExecutions(Long flowId, PageRequest pageRequest) {
        log.info("Listing executions for flow: flowId={}", flowId);
        Page<FlowExecutionEntity> page = new Page<>(pageRequest.getPage(), pageRequest.getSize());
        LambdaQueryWrapper<FlowExecutionEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FlowExecutionEntity::getFlowId, flowId);
        wrapper.orderByDesc(FlowExecutionEntity::getStartTime);
        IPage<FlowExecutionEntity> result = executionMapper.selectPage(page, wrapper);
        return PageResult.of(result.convert(this::toExecutionVO));
    }

    @Override
    public PageResult<FlowExecutionVO> listAllExecutions(PageRequest pageRequest, String status) {
        log.info("Listing all executions: status={}", status);
        Page<FlowExecutionEntity> page = new Page<>(pageRequest.getPage(), pageRequest.getSize());
        LambdaQueryWrapper<FlowExecutionEntity> wrapper = new LambdaQueryWrapper<>();
        FlowStatus statusEnum = parseStatus(status);
        if (statusEnum != null) {
            wrapper.eq(FlowExecutionEntity::getStatus, statusEnum);
        }
        wrapper.orderByDesc(FlowExecutionEntity::getStartTime);
        IPage<FlowExecutionEntity> result = executionMapper.selectPage(page, wrapper);
        return PageResult.of(result.convert(this::toExecutionVO));
    }

    /**
     * Maps frontend status aliases to backend FlowStatus enum, tolerating
     * unknown values by returning null (no status filter applied).
     */
    private FlowStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        String normalized = status.trim().toUpperCase();
        switch (normalized) {
            case "EXECUTING":
                return FlowStatus.RUNNING;
            case "ERROR":
                return FlowStatus.FAILED;
            default:
                try {
                    return FlowStatus.valueOf(normalized);
                } catch (IllegalArgumentException ex) {
                    log.warn("Unknown execution status filter ignored: {}", status);
                    return null;
                }
        }
    }

    private FlowEntity getFlowEntityOrThrow(Long id) {
        FlowEntity entity = flowMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Flow not found: " + id);
        }
        return entity;
    }

    private FlowVO toFlowVO(FlowEntity entity) {
        int nodeCount = 0;
        int edgeCount = 0;
        if (entity.getDefinition() != null) {
            if (entity.getDefinition().getNodes() != null) {
                nodeCount = entity.getDefinition().getNodes().size();
            }
            if (entity.getDefinition().getEdges() != null) {
                edgeCount = entity.getDefinition().getEdges().size();
            }
        }
        return FlowVO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .version(entity.getVersion())
                .definition(entity.getDefinition())
                .status(entity.getStatus())
                .category(entity.getCategory())
                .tags(entity.getTags())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .createBy(entity.getCreateBy())
                .nodeCount(nodeCount)
                .edgeCount(edgeCount)
                .build();
    }

    private FlowExecutionVO toExecutionVO(FlowExecutionEntity entity) {
        String flowName = null;
        if (entity.getFlowId() != null) {
            FlowEntity flow = flowMapper.selectById(entity.getFlowId());
            if (flow != null) {
                flowName = flow.getName();
            }
        }
        return FlowExecutionVO.builder()
                .id(entity.getId())
                .flowId(entity.getFlowId())
                .flowName(flowName)
                .flowVersion(entity.getFlowVersion())
                .status(entity.getStatus())
                .inputParams(entity.getInputParams())
                .outputResult(entity.getOutputResult())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .nodeExecutions(entity.getNodeExecutions())
                .errorMessage(entity.getErrorMessage())
                .createBy(entity.getCreateBy())
                .build();
    }
}
