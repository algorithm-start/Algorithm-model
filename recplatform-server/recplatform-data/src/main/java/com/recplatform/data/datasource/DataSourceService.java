package com.recplatform.data.datasource;

import com.recplatform.common.model.PageRequest;
import com.recplatform.common.model.PageResult;
import com.recplatform.data.datasource.dto.ConnectionTestResult;
import com.recplatform.data.datasource.dto.DataSourceCreateRequest;
import com.recplatform.data.datasource.dto.DataSourceVO;

import java.util.Map;

/**
 * Data source service interface.
 */
public interface DataSourceService {

    DataSourceVO create(DataSourceCreateRequest request);

    DataSourceVO get(Long id);

    DataSourceVO update(Long id, DataSourceCreateRequest request);

    void delete(Long id);

    PageResult<DataSourceVO> list(PageRequest pageRequest);

    ConnectionTestResult testConnection(Long id);

    ConnectionTestResult testConnection(DataSourceCreateRequest request);

    Map<String, Object> getMetadata(Long id);
}
