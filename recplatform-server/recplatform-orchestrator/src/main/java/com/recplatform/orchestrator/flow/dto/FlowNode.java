package com.recplatform.orchestrator.flow.dto;

import com.recplatform.common.enums.FlowNodeType;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * Represents a single node in a flow definition.
 */
@Data
public class FlowNode implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    private FlowNodeType type;

    private String name;

    private Map<String, Object> config;

    private NodePosition position;

    private Map<String, String> inputMappings;

    private Map<String, String> outputMappings;

    private RetryConfig retryConfig;

    private Integer timeout;
}
