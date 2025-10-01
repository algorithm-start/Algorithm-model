package com.recplatform.orchestrator.flow;

import com.recplatform.common.model.PageRequest;
import com.recplatform.common.model.PageResult;
import com.recplatform.orchestrator.flow.dto.FlowDefinition;
import com.recplatform.orchestrator.flow.dto.FlowRunRequest;
import com.recplatform.orchestrator.flow.vo.FlowExecutionVO;
import com.recplatform.orchestrator.flow.vo.FlowVO;

import java.util.Map;

/**
 * Service for managing and executing orchestration flows.
 */
public interface FlowService {

    /**
     * Create a new flow.
     */
    FlowVO createFlow(String name, String description, FlowDefinition definition, String category, java.util.List<String> tags);

    /**
     * List flows with pagination.
     */
    PageResult<FlowVO> listFlows(PageRequest pageRequest, String category, String status);

    /**
     * Get flow details by ID.
     */
    FlowVO getFlow(Long id);

    /**
     * Update flow definition.
     */
    FlowVO updateFlow(Long id, String name, String description, FlowDefinition definition, String category, java.util.List<String> tags);

    /**
     * Delete a flow (logical delete).
     */
    void deleteFlow(Long id);

    /**
     * Publish a flow (DRAFT -> PUBLISHED). Validates the DAG first.
     */
    FlowVO publishFlow(Long id);

    /**
     * Execute a flow with input parameters.
     */
    FlowExecutionVO executeFlow(Long id, Map<String, Object> inputParams);

    /**
     * Stop a running flow execution.
     */
    void stopFlow(Long executionId);

    /**
     * Get execution details with node-level status.
     */
    FlowExecutionVO getExecution(Long executionId);

    /**
     * List execution history for a flow.
     */
    PageResult<FlowExecutionVO> listExecutions(Long flowId, PageRequest pageRequest);

    /**
     * List execution history across all flows, optionally filtered by status.
     */
    PageResult<FlowExecutionVO> listAllExecutions(PageRequest pageRequest, String status);
}
