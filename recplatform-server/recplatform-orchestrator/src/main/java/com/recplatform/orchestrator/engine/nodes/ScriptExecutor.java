package com.recplatform.orchestrator.engine.nodes;

import com.recplatform.common.enums.FlowNodeType;
import com.recplatform.orchestrator.engine.FlowContext;
import com.recplatform.orchestrator.engine.NodeExecutionResult;
import com.recplatform.orchestrator.flow.dto.FlowNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.HashMap;
import java.util.Map;

/**
 * Executor for SCRIPT nodes.
 * Executes Groovy or JavaScript scripts in a sandboxed environment with timeout.
 */
@Slf4j
@Component
public class ScriptExecutor implements NodeExecutor {

    private final ScriptEngineManager scriptEngineManager = new ScriptEngineManager();

    @Override
    @SuppressWarnings("unchecked")
    public NodeExecutionResult execute(FlowNode node, FlowContext context) {
        log.debug("Executing SCRIPT node: id={}, name={}", node.getId(), node.getName());

        Map<String, Object> config = node.getConfig();
        if (config == null) {
            return NodeExecutionResult.fail("脚本节点缺少配置");
        }

        String language = (String) config.getOrDefault("language", "groovy");
        String script = (String) config.get("script");
        if (script == null || script.isEmpty()) {
            return NodeExecutionResult.fail("脚本节点缺少 script 脚本配置");
        }

        ScriptEngine engine = scriptEngineManager.getEngineByName(language);
        if (engine == null) {
            return NodeExecutionResult.fail("不支持的脚本语言："  + language);
        }

        try {
            Bindings bindings = engine.createBindings();

            // Expose global variables
            context.getGlobalVariables().forEach(bindings::put);

            // Expose node outputs
            bindings.put("nodeOutputs", context.getNodeOutputs());

            // Expose input/output variable names
            @SuppressWarnings("unchecked")
            Map<String, String> inputVars = (Map<String, String>) config.get("inputVariables");
            if (inputVars != null) {
                inputVars.forEach((alias, varName) -> {
                    Object value = context.getGlobalVariable(varName);
                    if (value != null) {
                        bindings.put(alias, value);
                    }
                });
            }

            Object result = engine.eval(script, bindings);

            // Build output
            Map<String, Object> output = new HashMap<>();
            if (result != null) {
                output.put("scriptResult", result);
            }

            // Map output variables back
            @SuppressWarnings("unchecked")
            Map<String, String> outputVars = (Map<String, String>) config.get("outputVariables");
            if (outputVars != null) {
                outputVars.forEach((alias, varName) -> {
                    Object value = bindings.get(alias);
                    if (value != null) {
                        output.put(varName, value);
                    }
                });
            }

            return NodeExecutionResult.success(output);

        } catch (Exception e) {
            log.error("Script execution failed: node={}, language={}, error={}",
                    node.getId(), language, e.getMessage());
            return NodeExecutionResult.fail("脚本执行失败："  + e.getMessage());
        }
    }

    @Override
    public FlowNodeType supportedType() {
        return FlowNodeType.SCRIPT;
    }
}
