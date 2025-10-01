package com.recplatform.data.pipeline;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.recplatform.common.enums.PipelineStatus;
import com.recplatform.common.exception.BusinessException;
import com.recplatform.common.model.BaseEntity;
import com.recplatform.common.model.PageRequest;
import com.recplatform.common.model.PageResult;
import com.recplatform.common.result.ResultCode;
import com.recplatform.common.util.JsonUtil;
import com.recplatform.data.pipeline.dto.PipelineCreateRequest;
import com.recplatform.data.pipeline.dto.PipelineExecutionVO;
import com.recplatform.data.pipeline.dto.PipelineVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Pipeline service implementation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PipelineServiceImpl implements PipelineService {

    private final PipelineMapper pipelineMapper;
    private final PipelineExecutionMapper executionMapper;
    private final PipelineExecutor pipelineExecutor;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PipelineVO create(PipelineCreateRequest request) {
        PipelineEntity entity = new PipelineEntity();
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setDefinition(JsonUtil.toJson(request.getDefinition()));
        entity.setMode(request.getMode());
        entity.setSchedule(request.getSchedule());
        entity.setStatus(PipelineStatus.DRAFT);
        pipelineMapper.insert(entity);
        log.info("Created pipeline: id={}, name={}", entity.getId(), entity.getName());
        return toVO(entity);
    }

    @Override
    public PipelineVO get(Long id) {
        PipelineEntity entity = getById(id);
        return toVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PipelineVO update(Long id, PipelineCreateRequest request) {
        PipelineEntity entity = getById(id);
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setDefinition(JsonUtil.toJson(request.getDefinition()));
        entity.setMode(request.getMode());
        entity.setSchedule(request.getSchedule());
        entity.setStatus(PipelineStatus.DRAFT);
        pipelineMapper.updateById(entity);
        log.info("Updated pipeline: id={}", id);
        return toVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        getById(id);
        pipelineMapper.deleteById(id);
        log.info("Deleted pipeline: id={}", id);
    }

    @Override
    public PageResult<PipelineVO> list(PageRequest pageRequest) {
        Page<PipelineEntity> page = new Page<>(pageRequest.getPage(), pageRequest.getSize());
        LambdaQueryWrapper<PipelineEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(BaseEntity::getCreateTime);
        Page<PipelineEntity> result = pipelineMapper.selectPage(page, wrapper);
        return PageResult.of(result.convert(this::toVO));
    }

    @Override
    public PipelineExecutionVO executePipeline(Long id) {
        PipelineEntity entity = getById(id);
        entity.setStatus(PipelineStatus.RUNNING);
        pipelineMapper.updateById(entity);

        pipelineExecutor.execute(entity);

        return PipelineExecutionVO.builder()
                .pipelineId(entity.getId())
                .status(ExecutionStatus.RUNNING)
                .build();
    }

    @Override
    public void stopPipeline(Long executionId) {
        pipelineExecutor.cancel(executionId);
    }

    @Override
    public PipelineExecutionVO getStatus(Long executionId) {
        PipelineExecutionEntity execution = executionMapper.selectById(executionId);
        if (execution == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Execution not found: " + executionId);
        }
        return toExecutionVO(execution);
    }

    @Override
    public List<PipelineExecutionVO> getHistory(Long pipelineId) {
        LambdaQueryWrapper<PipelineExecutionEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PipelineExecutionEntity::getPipelineId, pipelineId)
               .orderByDesc(PipelineExecutionEntity::getStartTime);
        List<PipelineExecutionEntity> executions = executionMapper.selectList(wrapper);
        return executions.stream().map(this::toExecutionVO).collect(Collectors.toList());
    }

    private PipelineEntity getById(Long id) {
        PipelineEntity entity = pipelineMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Pipeline not found: " + id);
        }
        return entity;
    }

    private PipelineVO toVO(PipelineEntity entity) {
        com.recplatform.data.pipeline.dto.PipelineDefinition definition = null;
        if (entity.getDefinition() != null) {
            definition = JsonUtil.fromJson(entity.getDefinition(),
                    com.recplatform.data.pipeline.dto.PipelineDefinition.class);
        }
        return PipelineVO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .definition(definition)
                .mode(entity.getMode())
                .schedule(entity.getSchedule())
                .status(entity.getStatus())
                .lastRunTime(entity.getLastRunTime())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }

    @SuppressWarnings("unchecked")
    private PipelineExecutionVO toExecutionVO(PipelineExecutionEntity entity) {
        Map<String, String> nodeStatuses = null;
        if (entity.getNodeStatuses() != null) {
            nodeStatuses = JsonUtil.fromJson(entity.getNodeStatuses(),
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {});
        }
        Map<String, Object> metrics = null;
        if (entity.getMetrics() != null) {
            metrics = JsonUtil.fromJson(entity.getMetrics(),
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        }
        return PipelineExecutionVO.builder()
                .id(entity.getId())
                .pipelineId(entity.getPipelineId())
                .status(entity.getStatus())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .nodeStatuses(nodeStatuses)
                .errorMessage(entity.getErrorMessage())
                .metrics(metrics)
                .build();
    }
}
