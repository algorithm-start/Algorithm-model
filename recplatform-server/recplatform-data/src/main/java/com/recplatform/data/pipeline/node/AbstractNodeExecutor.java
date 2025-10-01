package com.recplatform.data.pipeline.node;

import com.recplatform.data.pipeline.dto.PipelineNode;

import java.util.List;
import java.util.Map;

/**
 * Abstract base class for pipeline node executors.
 */
public abstract class AbstractNodeExecutor {

    /**
     * Execute the node with the given configuration and input data.
     *
     * @param node      the pipeline node definition
     * @param inputData list of input data rows from upstream nodes
     * @return output data rows
     */
    public abstract List<Map<String, Object>> execute(PipelineNode node, List<Map<String, Object>> inputData);
}
