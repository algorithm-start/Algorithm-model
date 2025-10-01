package com.recplatform.orchestrator.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.recplatform.common.exception.BusinessException;
import com.recplatform.common.model.PageRequest;
import com.recplatform.common.model.PageResult;
import com.recplatform.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Service for managing the service registry.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceRegistryService {

    private final RegisteredServiceMapper serviceMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Transactional(rollbackFor = Exception.class)
    public RegisteredServiceEntity registerService(RegisteredServiceEntity entity) {
        log.info("Registering service: name={}, type={}", entity.getName(), entity.getType());
        entity.setStatus("ACTIVE");
        serviceMapper.insert(entity);
        return entity;
    }

    public PageResult<RegisteredServiceEntity> listServices(PageRequest pageRequest, String type, String status) {
        Page<RegisteredServiceEntity> page = new Page<>(pageRequest.getPage(), pageRequest.getSize());
        LambdaQueryWrapper<RegisteredServiceEntity> wrapper = new LambdaQueryWrapper<>();
        if (type != null && !type.isEmpty()) {
            wrapper.eq(RegisteredServiceEntity::getType, type);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(RegisteredServiceEntity::getStatus, status);
        }
        wrapper.orderByDesc(RegisteredServiceEntity::getCreateTime);
        IPage<RegisteredServiceEntity> result = serviceMapper.selectPage(page, wrapper);
        return PageResult.of(result);
    }

    public RegisteredServiceEntity getService(Long id) {
        return getServiceOrThrow(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public RegisteredServiceEntity updateService(Long id, RegisteredServiceEntity updates) {
        log.info("Updating service: id={}", id);
        RegisteredServiceEntity entity = getServiceOrThrow(id);
        if (updates.getName() != null) entity.setName(updates.getName());
        if (updates.getDescription() != null) entity.setDescription(updates.getDescription());
        if (updates.getType() != null) entity.setType(updates.getType());
        if (updates.getBaseUrl() != null) entity.setBaseUrl(updates.getBaseUrl());
        if (updates.getHealthCheckPath() != null) entity.setHealthCheckPath(updates.getHealthCheckPath());
        if (updates.getAuthConfig() != null) entity.setAuthConfig(updates.getAuthConfig());
        if (updates.getInputSchema() != null) entity.setInputSchema(updates.getInputSchema());
        if (updates.getOutputSchema() != null) entity.setOutputSchema(updates.getOutputSchema());
        serviceMapper.updateById(entity);
        return entity;
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteService(Long id) {
        log.info("Deleting service: id={}", id);
        getServiceOrThrow(id);
        serviceMapper.deleteById(id);
    }

    /**
     * Perform a health check on the service.
     */
    public Map<String, Object> testService(Long id) {
        log.info("Testing service health: id={}", id);
        RegisteredServiceEntity entity = getServiceOrThrow(id);
        String healthUrl = entity.getBaseUrl();
        if (entity.getHealthCheckPath() != null && !entity.getHealthCheckPath().isEmpty()) {
            healthUrl = healthUrl + entity.getHealthCheckPath();
        }

        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(healthUrl, Map.class);
            entity.setStatus("ACTIVE");
            entity.setLastHealthCheck(LocalDateTime.now());
            serviceMapper.updateById(entity);
            return Map.of("healthy", true, "statusCode", response.getStatusCode().value());
        } catch (Exception e) {
            log.warn("Service health check failed: id={}, error={}", id, e.getMessage());
            entity.setStatus("INACTIVE");
            entity.setLastHealthCheck(LocalDateTime.now());
            serviceMapper.updateById(entity);
            return Map.of("healthy", false, "error", e.getMessage());
        }
    }

    private RegisteredServiceEntity getServiceOrThrow(Long id) {
        RegisteredServiceEntity entity = serviceMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Service not found: " + id);
        }
        return entity;
    }
}
