package com.recplatform.data.pipeline;

import com.recplatform.common.model.PageRequest;
import com.recplatform.common.model.PageResult;
import com.recplatform.data.pipeline.dto.PipelineCreateRequest;
import com.recplatform.data.pipeline.dto.PipelineExecutionVO;
import com.recplatform.data.pipeline.dto.PipelineVO;

import java.util.List;

/**
 * Pipeline service interface.
 */
public interface PipelineService {

    PipelineVO create(PipelineCreateRequest request);

    PipelineVO get(Long id);

    PipelineVO update(Long id, PipelineCreateRequest request);

    void delete(Long id);

    PageResult<PipelineVO> list(PageRequest pageRequest);

    PipelineExecutionVO executePipeline(Long id);

    void stopPipeline(Long executionId);

    PipelineExecutionVO getStatus(Long executionId);

    List<PipelineExecutionVO> getHistory(Long pipelineId);
}
