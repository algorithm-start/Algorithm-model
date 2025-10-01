package com.recplatform.data.api;

import com.recplatform.common.exception.BusinessException;
import com.recplatform.common.result.ResultCode;
import com.recplatform.common.util.JsonUtil;
import com.recplatform.data.api.dto.DataApiCreateRequest;
import com.recplatform.data.api.dto.DataApiVO;
import com.recplatform.data.datasource.DataSourceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Data API service implementation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataApiServiceImpl implements DataApiService {

    private final DataApiMapper dataApiMapper;
    private final DataSourceMapper dataSourceMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DataApiVO create(DataApiCreateRequest request) {
        DataApiEntity entity = new DataApiEntity();
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setPath(request.getPath());
        entity.setMethod(request.getMethod());
        entity.setDataSourceId(request.getDataSourceId());
        entity.setQuery(request.getQuery());
        if (request.getParameters() != null) {
            entity.setParameters(JsonUtil.toJson(request.getParameters()));
        }
        if (request.getResponseMapping() != null) {
            entity.setResponseMapping(JsonUtil.toJson(request.getResponseMapping()));
        }
        entity.setStatus(DataApiStatus.DRAFT);
        entity.setAuthRequired(request.getAuthRequired() != null ? request.getAuthRequired() : false);
        entity.setRateLimit(request.getRateLimit());
        entity.setCallCount(0L);
        dataApiMapper.insert(entity);
        log.info("Created data API: id={}, name={}", entity.getId(), entity.getName());
        return toVO(entity);
    }

    @Override
    public DataApiVO get(Long id) {
        DataApiEntity entity = getById(id);
        return toVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DataApiVO update(Long id, DataApiCreateRequest request) {
        DataApiEntity entity = getById(id);
        if (entity.getStatus() == DataApiStatus.PUBLISHED) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "Cannot update a published API. Please unpublish first.");
        }
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setPath(request.getPath());
        entity.setMethod(request.getMethod());
        entity.setDataSourceId(request.getDataSourceId());
        entity.setQuery(request.getQuery());
        if (request.getParameters() != null) {
            entity.setParameters(JsonUtil.toJson(request.getParameters()));
        }
        if (request.getResponseMapping() != null) {
            entity.setResponseMapping(JsonUtil.toJson(request.getResponseMapping()));
        }
        entity.setAuthRequired(request.getAuthRequired());
        entity.setRateLimit(request.getRateLimit());
        dataApiMapper.updateById(entity);
        log.info("Updated data API: id={}", id);
        return toVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        DataApiEntity entity = getById(id);
        if (entity.getStatus() == DataApiStatus.PUBLISHED) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "Cannot delete a published API. Please unpublish first.");
        }
        dataApiMapper.deleteById(id);
        log.info("Deleted data API: id={}", id);
    }

    @Override
    public List<DataApiVO> list() {
        List<DataApiEntity> entities = dataApiMapper.selectList(null);
        return entities.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DataApiVO publish(Long id) {
        DataApiEntity entity = getById(id);
        validateForPublish(entity);
        entity.setStatus(DataApiStatus.PUBLISHED);
        dataApiMapper.updateById(entity);
        log.info("Published data API: id={}, path={}", id, entity.getPath());
        return toVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DataApiVO unpublish(Long id) {
        DataApiEntity entity = getById(id);
        entity.setStatus(DataApiStatus.DEPRECATED);
        dataApiMapper.updateById(entity);
        log.info("Unpublished data API: id={}", id);
        return toVO(entity);
    }

    @Override
    public Map<String, Object> test(Long id, Map<String, Object> params) {
        DataApiEntity entity = getById(id);
        Map<String, Object> result = new HashMap<>();
        result.put("apiId", id);
        result.put("apiName", entity.getName());
        result.put("query", entity.getQuery());
        result.put("params", params);
        result.put("message", "API test executed (placeholder)");
        return result;
    }

    @Override
    public Map<String, Object> getStats(Long id) {
        DataApiEntity entity = getById(id);
        Map<String, Object> stats = new HashMap<>();
        stats.put("apiId", id);
        stats.put("callCount", entity.getCallCount());
        stats.put("status", entity.getStatus().name());
        return stats;
    }

    private DataApiEntity getById(Long id) {
        DataApiEntity entity = dataApiMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Data API not found: " + id);
        }
        return entity;
    }

    private void validateForPublish(DataApiEntity entity) {
        if (entity.getPath() == null || entity.getPath().isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "API path is required for publishing");
        }
        if (entity.getQuery() == null || entity.getQuery().isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "API query is required for publishing");
        }
        if (entity.getDataSourceId() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Data source is required for publishing");
        }
    }

    @SuppressWarnings("unchecked")
    private DataApiVO toVO(DataApiEntity entity) {
        Map<String, Object> parameters = null;
        if (entity.getParameters() != null) {
            parameters = JsonUtil.fromJson(entity.getParameters(),
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        }
        Map<String, Object> responseMapping = null;
        if (entity.getResponseMapping() != null) {
            responseMapping = JsonUtil.fromJson(entity.getResponseMapping(),
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        }
        return DataApiVO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .path(entity.getPath())
                .method(entity.getMethod())
                .dataSourceId(entity.getDataSourceId())
                .query(entity.getQuery())
                .parameters(parameters)
                .responseMapping(responseMapping)
                .status(entity.getStatus())
                .authRequired(entity.getAuthRequired())
                .rateLimit(entity.getRateLimit())
                .callCount(entity.getCallCount())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }
}
