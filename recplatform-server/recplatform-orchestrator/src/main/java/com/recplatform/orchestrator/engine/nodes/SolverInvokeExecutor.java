package com.recplatform.orchestrator.engine.nodes;

import com.recplatform.common.enums.FlowNodeType;
import com.recplatform.orchestrator.engine.FlowContext;
import com.recplatform.orchestrator.engine.NodeExecutionResult;
import com.recplatform.orchestrator.flow.dto.FlowNode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.Map;

/**
 * Executor for SOLVER_INVOKE nodes.
 * Calls the solver API to solve an optimization problem.
 */
@Slf4j
@Component
public class SolverInvokeExecutor implements NodeExecutor {

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public NodeExecutionResult execute(FlowNode node, FlowContext context) {
        log.debug("Executing SOLVER_INVOKE node: id={}, name={}", node.getId(), node.getName());

        Map<String, Object> config = node.getConfig();
        if (config == null) {
            return NodeExecutionResult.fail("求解器调用节点缺少配置");
        }

        Object problemIdObj = config.get("problemId");
        if (problemIdObj == null || problemIdObj.toString().isEmpty()) {
            return NodeExecutionResult.fail("求解器调用节点缺少 problemId 配置");
        }

        // problemId may arrive as a number or a string from the flow config.
        // The solve endpoint identifies a problem by its numeric id, so require a
        // numeric value and give a clear message when a non-numeric code is used.
        long problemId;
        try {
            problemId = problemIdObj instanceof Number
                    ? ((Number) problemIdObj).longValue()
                    : Long.parseLong(problemIdObj.toString().trim());
        } catch (NumberFormatException e) {
            return NodeExecutionResult.fail(
                    "求解器调用节点的 problemId 必须是数字问题 ID（当前为：" + problemIdObj
                            + "）。请在问题列表中选择一个已存在的问题。");
        }

        String baseUrl = (String) config.getOrDefault("baseUrl", "http://localhost:8080");
        String solveUrl = baseUrl + "/api/v1/solver/problems/" + problemId + "/solve";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            // Forward the caller's auth token so the internal solver API call
            // passes Sa-Token authentication (the flow runs on the request thread).
            forwardAuthToken(headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> solverConfig = (Map<String, Object>) config.get("solverConfig");
            // The solve endpoint binds @RequestBody SolverConfig and rejects an
            // empty body with 400, so send an empty object when no config is set.
            if (solverConfig == null) {
                solverConfig = new HashMap<>();
            }
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(solverConfig, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    solveUrl, HttpMethod.POST, entity, Map.class);

            Map<String, Object> output = new HashMap<>();
            output.put("problemId", problemId);
            if (response.getBody() != null) {
                output.put("solverResult", response.getBody());
            }
            return NodeExecutionResult.success(output);

        } catch (Exception e) {
            log.error("Solver invoke failed: problemId={}, error={}", problemId, e.getMessage());
            return NodeExecutionResult.fail("求解器调用失败："  + e.getMessage());
        }
    }

    /**
     * Copy the current request's Authorization header onto the outgoing internal
     * call so the solver API (protected by Sa-Token) accepts it. Safe no-op when
     * there is no active request context.
     */
    private void forwardAuthToken(HttpHeaders headers) {
        var attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
            return;
        }
        HttpServletRequest request = servletAttributes.getRequest();
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && !authHeader.isEmpty()) {
            headers.set(HttpHeaders.AUTHORIZATION, authHeader);
            return;
        }
        // Some clients send the token via the "satoken" header instead of Authorization.
        String saToken = request.getHeader("satoken");
        if (saToken != null && !saToken.isEmpty()) {
            headers.set("satoken", saToken);
        }
    }

    @Override
    public FlowNodeType supportedType() {
        return FlowNodeType.SOLVER_INVOKE;
    }
}
