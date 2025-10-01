package com.recplatform.orchestrator.flow.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * Request to run a flow with input parameters.
 */
@Data
public class FlowRunRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Map<String, Object> inputParams;
}
