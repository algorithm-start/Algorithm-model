package com.recplatform.orchestrator.flow.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * Retry configuration for flow nodes.
 */
@Data
public class RetryConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    private int maxRetries = 0;

    private long retryDelay = 1000;
}
