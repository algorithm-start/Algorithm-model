package com.recplatform.orchestrator.flow.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Complete flow definition containing nodes, edges, and global parameters.
 */
@Data
public class FlowDefinition implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<FlowNode> nodes;

    private List<FlowEdge> edges;

    private Map<String, Object> globalParams;
}
