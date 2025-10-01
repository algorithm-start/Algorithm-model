package com.recplatform.data.api;

import com.recplatform.data.api.dto.DataApiCreateRequest;
import com.recplatform.data.api.dto.DataApiVO;

import java.util.List;
import java.util.Map;

/**
 * Data API service interface.
 */
public interface DataApiService {

    DataApiVO create(DataApiCreateRequest request);

    DataApiVO get(Long id);

    DataApiVO update(Long id, DataApiCreateRequest request);

    void delete(Long id);

    List<DataApiVO> list();

    DataApiVO publish(Long id);

    DataApiVO unpublish(Long id);

    Map<String, Object> test(Long id, Map<String, Object> params);

    Map<String, Object> getStats(Long id);
}
