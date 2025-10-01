package com.recplatform.data.datasource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.recplatform.common.exception.BusinessException;
import com.recplatform.common.model.BaseEntity;
import com.recplatform.common.model.PageRequest;
import com.recplatform.common.model.PageResult;
import com.recplatform.common.result.ResultCode;
import com.recplatform.common.util.JsonUtil;
import com.recplatform.data.datasource.dto.ConnectionTestResult;
import com.recplatform.data.datasource.dto.DataSourceCreateRequest;
import com.recplatform.data.datasource.dto.DataSourceVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Data source service implementation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataSourceServiceImpl implements DataSourceService {

    private final DataSourceMapper dataSourceMapper;
    private final DataSourceConnectionFactory connectionFactory;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DataSourceVO create(DataSourceCreateRequest request) {
        DataSourceEntity entity = new DataSourceEntity();
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setType(request.getType());
        entity.setStatus(DataSourceStatus.ACTIVE);

        Map<String, Object> config = request.getConnectionConfig();
        if (config != null) {
            entity.setHost((String) config.get("host"));
            entity.setPort(config.get("port") != null ? ((Number) config.get("port")).intValue() : null);
            entity.setDatabase((String) config.get("database"));
            entity.setUsername((String) config.get("username"));
            entity.setPassword((String) config.get("password"));
            Object options = config.get("options");
            if (options != null) {
                entity.setOptions(JsonUtil.toJson(options));
            }
        }

        dataSourceMapper.insert(entity);
        log.info("Created data source: id={}, name={}", entity.getId(), entity.getName());
        return toVO(entity);
    }

    @Override
    public DataSourceVO get(Long id) {
        DataSourceEntity entity = getById(id);
        return toVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DataSourceVO update(Long id, DataSourceCreateRequest request) {
        DataSourceEntity entity = getById(id);
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setType(request.getType());

        Map<String, Object> config = request.getConnectionConfig();
        if (config != null) {
            entity.setHost((String) config.get("host"));
            entity.setPort(config.get("port") != null ? ((Number) config.get("port")).intValue() : null);
            entity.setDatabase((String) config.get("database"));
            entity.setUsername((String) config.get("username"));
            String newPassword = (String) config.get("password");
            if (newPassword != null && !newPassword.isBlank() && !MASKED_PASSWORD.equals(newPassword)) {
                entity.setPassword(newPassword);
            }
            Object options = config.get("options");
            if (options != null) {
                entity.setOptions(JsonUtil.toJson(options));
            }
        }

        dataSourceMapper.updateById(entity);
        log.info("Updated data source: id={}", id);
        return toVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        DataSourceEntity entity = getById(id);
        dataSourceMapper.deleteById(entity.getId());
        log.info("Deleted data source: id={}", id);
    }

    @Override
    public PageResult<DataSourceVO> list(PageRequest pageRequest) {
        Page<DataSourceEntity> page = new Page<>(pageRequest.getPage(), pageRequest.getSize());
        LambdaQueryWrapper<DataSourceEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(BaseEntity::getCreateTime);
        Page<DataSourceEntity> result = dataSourceMapper.selectPage(page, wrapper);
        return PageResult.of(result.convert(this::toVO));
    }

    private static final int MAX_TEST_RESULT_LENGTH = 500;

    private static final String MASKED_PASSWORD = "******";

    @Override
    public ConnectionTestResult testConnection(Long id) {
        DataSourceEntity entity = getById(id);
        ConnectionTestResult result = connectionFactory.testConnection(entity);
        entity.setLastTestTime(LocalDateTime.now());
        String testResult = result.isSuccess() ? "SUCCESS" : "FAILED: " + result.getMessage();
        if (testResult.length() > MAX_TEST_RESULT_LENGTH) {
            testResult = testResult.substring(0, MAX_TEST_RESULT_LENGTH);
        }
        entity.setLastTestResult(testResult);
        dataSourceMapper.updateById(entity);
        return result;
    }

    @Override
    public ConnectionTestResult testConnection(DataSourceCreateRequest request) {
        DataSourceEntity entity = new DataSourceEntity();
        entity.setName(request.getName());
        entity.setType(request.getType());

        Map<String, Object> config = request.getConnectionConfig();
        if (config != null) {
            entity.setHost((String) config.get("host"));
            entity.setPort(config.get("port") != null ? ((Number) config.get("port")).intValue() : null);
            entity.setDatabase((String) config.get("database"));
            entity.setUsername((String) config.get("username"));
            entity.setPassword((String) config.get("password"));
        }
        return connectionFactory.testConnection(entity);
    }

    @Override
    public Map<String, Object> getMetadata(Long id) {
        DataSourceEntity entity = getById(id);
        return connectionFactory.getMetadata(entity);
    }

    private DataSourceEntity getById(Long id) {
        DataSourceEntity entity = dataSourceMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Data source not found: " + id);
        }
        return entity;
    }

    private DataSourceVO toVO(DataSourceEntity entity) {
        return DataSourceVO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .type(entity.getType())
                .host(entity.getHost())
                .port(entity.getPort())
                .database(entity.getDatabase())
                .username(entity.getUsername())
                .password(entity.getPassword() != null ? MASKED_PASSWORD : null)
                .status(entity.getStatus())
                .lastTestTime(entity.getLastTestTime())
                .lastTestResult(entity.getLastTestResult())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }
}
