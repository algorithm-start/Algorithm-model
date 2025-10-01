package com.recplatform.solver.service;

import com.recplatform.common.enums.SolverStatus;
import com.recplatform.solver.config.SolverProperties;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.io.Serializable;
import java.util.Map;

/**
 * REST client for communicating with the Python solver-engine service.
 */
@Slf4j
@Component
public class SolverEngineClient {

    private final SolverProperties properties;
    private RestTemplate restTemplate;

    public SolverEngineClient(SolverProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(properties.getDefaultTimeout() * 1000);
        this.restTemplate = new RestTemplate(factory);
        log.info("SolverEngineClient initialized with engine URL: {}", properties.getEngineUrl());
    }

    /**
     * Submit a solve request to the Python engine.
     *
     * @param request the solver request containing problem and config
     * @return the solver response
     */
    public SolverResponse submitSolve(SolverRequest request) {
        String url = properties.getEngineUrl() + "/solve";
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<SolverRequest> entity = new HttpEntity<>(request, headers);

            log.info("Submitting solve request to {}: problem={}", url, request.getProblem() != null ? "provided" : "null");
            SolverResponse response = restTemplate.postForObject(url, entity, SolverResponse.class);

            if (response == null) {
                throw new IllegalStateException("Solver engine returned null response");
            }
            log.info("Solve completed: status={}, objectiveValue={}, solveTime={}s",
                    response.getStatus(), response.getObjectiveValue(), response.getSolveTime());
            return response;
        } catch (RestClientException e) {
            log.error("Failed to submit solve request to {}: {}", url, e.getMessage());
            SolverResponse errorResponse = new SolverResponse();
            errorResponse.setStatus("error");
            errorResponse.setErrorMessage("Solver engine unreachable: " + e.getMessage());
            return errorResponse;
        }
    }

    /**
     * Get the status of an async job from the engine.
     *
     * @param jobId the job ID to check
     * @return the solver status response
     */
    public SolverStatusResponse getStatus(String jobId) {
        String url = properties.getEngineUrl() + "/solve/" + jobId + "/status";
        try {
            SolverStatusResponse response = restTemplate.getForObject(url, SolverStatusResponse.class);
            if (response == null) {
                throw new IllegalStateException("Solver engine returned null status response");
            }
            return response;
        } catch (RestClientException e) {
            log.error("Failed to get status for job {} from {}: {}", jobId, url, e.getMessage());
            SolverStatusResponse errorResponse = new SolverStatusResponse();
            errorResponse.setJobId(jobId);
            errorResponse.setStatus("error");
            errorResponse.setErrorMessage("Solver engine unreachable: " + e.getMessage());
            return errorResponse;
        }
    }

    /**
     * Cancel a running job on the engine.
     *
     * @param jobId the job ID to cancel
     */
    public void cancel(String jobId) {
        String url = properties.getEngineUrl() + "/solve/" + jobId + "/cancel";
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Void> entity = new HttpEntity<>(null, headers);
            restTemplate.postForObject(url, entity, Void.class);
            log.info("Cancel request sent for job {}", jobId);
        } catch (RestClientException e) {
            log.error("Failed to cancel job {} at {}: {}", jobId, url, e.getMessage());
        }
    }

    // --- Inner DTO classes for engine communication ---

    /**
     * Request body sent to the Python solver engine.
     */
    @Data
    public static class SolverRequest implements Serializable {

        private static final long serialVersionUID = 1L;

        private Map<String, Object> problem;
        private Map<String, Object> config;
    }

    /**
     * Response body from the Python solver engine /solve endpoint.
     */
    @Data
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    @com.fasterxml.jackson.databind.annotation.JsonNaming(com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class SolverResponse implements Serializable {

        private static final long serialVersionUID = 1L;

        private String status;
        private Double objectiveValue;
        private Map<String, Double> variables;
        private Double solveTime;
        private String problemType;
        private String solverUsed;
        private String algorithm;
        private String errorMessage;
    }

    /**
     * Status response from the Python solver engine.
     */
    @Data
    public static class SolverStatusResponse implements Serializable {

        private static final long serialVersionUID = 1L;

        private String jobId;
        private String status;
        private String errorMessage;
    }
}
