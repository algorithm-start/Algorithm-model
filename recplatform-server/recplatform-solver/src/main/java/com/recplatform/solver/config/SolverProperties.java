package com.recplatform.solver.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for the solver module.
 */
@Data
@Component
@ConfigurationProperties(prefix = "recplatform.solver")
public class SolverProperties {

    /**
     * Base URL for the Python solver engine.
     */
    private String engineUrl = "http://localhost:8001";

    /**
     * Default solver timeout in seconds.
     */
    private int defaultTimeout = 300;

    /**
     * Maximum number of concurrent solver jobs.
     */
    private int maxConcurrentJobs = 10;
}
