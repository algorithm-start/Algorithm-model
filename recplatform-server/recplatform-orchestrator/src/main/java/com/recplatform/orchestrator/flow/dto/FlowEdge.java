package com.recplatform.orchestrator.flow.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * Represents an edge connecting two nodes in a flow.
 */
@Data
public class FlowEdge implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    private String source;

    private String target;

    private String condition;
}
