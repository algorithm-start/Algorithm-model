package com.recplatform.data.pipeline.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Pipeline DAG definition containing nodes and edges.
 */
@Data
public class PipelineDefinition implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<PipelineNode> nodes;
    private List<PipelineEdge> edges;
}
