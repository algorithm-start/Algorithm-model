package com.recplatform.orchestrator.engine.nodes;

import com.recplatform.common.enums.FlowNodeType;
import com.recplatform.orchestrator.engine.FlowContext;
import com.recplatform.orchestrator.engine.NodeExecutionResult;
import com.recplatform.orchestrator.flow.dto.FlowNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Executor for SERVICE_CALL nodes.
 * Calls external REST/gRPC services with configured URL, method, headers, and body template.
 */
@Slf4j
@Component
public class ServiceCallExecutor implements NodeExecutor {

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public NodeExecutionResult execute(FlowNode node, FlowContext context) {
        log.debug("Executing SERVICE_CALL node: id={}, name={}", node.getId(), node.getName());

        Map<String, Object> config = node.getConfig();
        if (config == null) {
            return NodeExecutionResult.fail("服务调用节点缺少配置");
        }

        String url = resolveUrl(config);
        String method = (String) config.getOrDefault("method", "POST");

        if (url == null || url.isEmpty()) {
            return NodeExecutionResult.fail("服务调用节点缺少 url 配置（请填写完整 URL，或同时填写服务名称与 Endpoint）");
        }

        try {
            // Build headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            @SuppressWarnings("unchecked")
            Map<String, String> configHeaders = (Map<String, String>) config.get("headers");
            if (configHeaders != null) {
                configHeaders.forEach(headers::set);
            }

            // Build body from template, substituting context variables
            Object body = config.get("body");
            if (body instanceof Map) {
                body = resolveVariables((Map<String, Object>) body, context);
            }

            HttpEntity<Object> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response;
            HttpMethod httpMethod = HttpMethod.valueOf(method.toUpperCase());
            response = restTemplate.exchange(url, httpMethod, entity, Map.class);

            Map<String, Object> output = new HashMap<>();
            output.put("statusCode", response.getStatusCode().value());
            if (response.getBody() != null) {
                output.put("response", response.getBody());
            }
            return NodeExecutionResult.success(output);

        } catch (Exception e) {
            log.error("Service call failed: url={}, error={}", url, e.getMessage());
            return NodeExecutionResult.fail("服务调用失败："  + e.getMessage());
        }
    }

    /**
     * Resolve the request URL from config.
     *
     * <p>Prefers the explicit {@code url} field. For backward compatibility with
     * flows configured using the older {@code serviceName} + {@code endpoint}
     * fields, falls back to joining them into a URL.
     */
    private String resolveUrl(Map<String, Object> config) {
        String url = (String) config.get("url");
        if (url != null && !url.isEmpty()) {
            return url;
        }

        String serviceName = (String) config.get("serviceName");
        String endpoint = (String) config.get("endpoint");
        if (serviceName == null || serviceName.isEmpty()) {
            return null;
        }

        String host = serviceName.startsWith("http") ? serviceName : "http://" + serviceName;
        if (endpoint == null || endpoint.isEmpty()) {
            return host;
        }
        String normalizedEndpoint = endpoint.startsWith("/") ? endpoint : "/" + endpoint;
        return host + normalizedEndpoint;
    }

    /**
     * Resolve ${var} references in the body template against the flow context.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> resolveVariables(Map<String, Object> template, FlowContext context) {
        Map<String, Object> resolved = new HashMap<>();
        for (Map.Entry<String, Object> entry : template.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String) {
                String strValue = (String) value;
                if (strValue.startsWith("${") && strValue.endsWith("}")) {
                    String varName = strValue.substring(2, strValue.length() - 1);
                    resolved.put(entry.getKey(), context.getGlobalVariable(varName));
                } else {
                    resolved.put(entry.getKey(), value);
                }
            } else if (value instanceof Map) {
                resolved.put(entry.getKey(), resolveVariables((Map<String, Object>) value, context));
            } else {
                resolved.put(entry.getKey(), value);
            }
        }
        return resolved;
    }

    @Override
    public FlowNodeType supportedType() {
        return FlowNodeType.SERVICE_CALL;
    }
}
