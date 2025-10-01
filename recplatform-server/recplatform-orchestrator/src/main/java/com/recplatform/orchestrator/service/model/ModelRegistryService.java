package com.recplatform.orchestrator.service.model;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.recplatform.common.exception.BusinessException;
import com.recplatform.common.model.PageRequest;
import com.recplatform.common.model.PageResult;
import com.recplatform.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing the model registry.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelRegistryService {

    private final RegisteredModelMapper modelMapper;

    @Transactional(rollbackFor = Exception.class)
    public RegisteredModelEntity registerModel(RegisteredModelEntity entity) {
        log.info("Registering model: name={}, algorithmType={}", entity.getName(), entity.getAlgorithmType());
        if (entity.getVersion() == null) {
            entity.setVersion(1);
        }
        entity.setStatus("ACTIVE");
        modelMapper.insert(entity);
        return entity;
    }

    public PageResult<RegisteredModelEntity> listModels(PageRequest pageRequest, String algorithmType, String status) {
        Page<RegisteredModelEntity> page = new Page<>(pageRequest.getPage(), pageRequest.getSize());
        LambdaQueryWrapper<RegisteredModelEntity> wrapper = new LambdaQueryWrapper<>();
        if (algorithmType != null && !algorithmType.isEmpty()) {
            wrapper.eq(RegisteredModelEntity::getAlgorithmType, algorithmType);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(RegisteredModelEntity::getStatus, status);
        }
        wrapper.orderByDesc(RegisteredModelEntity::getCreateTime);
        IPage<RegisteredModelEntity> result = modelMapper.selectPage(page, wrapper);
        return PageResult.of(result);
    }

    public RegisteredModelEntity getModel(Long id) {
        return getModelOrThrow(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public RegisteredModelEntity updateModel(Long id, RegisteredModelEntity updates) {
        log.info("Updating model: id={}", id);
        RegisteredModelEntity entity = getModelOrThrow(id);
        if (updates.getName() != null) entity.setName(updates.getName());
        if (updates.getDescription() != null) entity.setDescription(updates.getDescription());
        if (updates.getSolverProblemId() != null) entity.setSolverProblemId(updates.getSolverProblemId());
        if (updates.getAlgorithmType() != null) entity.setAlgorithmType(updates.getAlgorithmType());
        if (updates.getDefaultConfig() != null) entity.setDefaultConfig(updates.getDefaultConfig());
        if (updates.getInputSchema() != null) entity.setInputSchema(updates.getInputSchema());
        if (updates.getOutputSchema() != null) entity.setOutputSchema(updates.getOutputSchema());
        if (updates.getVersion() != null) entity.setVersion(updates.getVersion());
        modelMapper.updateById(entity);
        return entity;
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteModel(Long id) {
        log.info("Deleting model: id={}", id);
        getModelOrThrow(id);
        modelMapper.deleteById(id);
    }

    private RegisteredModelEntity getModelOrThrow(Long id) {
        RegisteredModelEntity entity = modelMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Model not found: " + id);
        }
        return entity;
    }
}
