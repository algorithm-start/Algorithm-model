package com.recplatform.data.pipeline.node;

import com.recplatform.data.pipeline.NodeSubType;
import com.recplatform.data.pipeline.dto.PipelineNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Executor for TRANSFORM type nodes.
 */
@Slf4j
@Component
public class TransformNodeExecutor extends AbstractNodeExecutor {

    @Override
    public List<Map<String, Object>> execute(PipelineNode node, List<Map<String, Object>> inputData) {
        NodeSubType subType = node.getSubType();
        log.info("Executing transform node: id={}, subType={}", node.getId(), subType);
        return switch (subType) {
            case FILTER -> executeFilter(node, inputData);
            case MAP -> executeMap(node, inputData);
            case JOIN -> executeJoin(node, inputData);
            case AGGREGATE -> executeAggregate(node, inputData);
            case SCRIPT -> executeScript(node, inputData);
            default -> {
                log.warn("Unsupported transform sub-type: {}", subType);
                yield inputData;
            }
        };
    }

    private List<Map<String, Object>> executeFilter(PipelineNode node, List<Map<String, Object>> inputData) {
        log.info("FILTER transform - node: {}, inputRows: {}", node.getName(), inputData.size());
        return inputData;
    }

    private List<Map<String, Object>> executeMap(PipelineNode node, List<Map<String, Object>> inputData) {
        log.info("MAP transform - node: {}, inputRows: {}", node.getName(), inputData.size());
        return inputData;
    }

    private List<Map<String, Object>> executeJoin(PipelineNode node, List<Map<String, Object>> inputData) {
        log.info("JOIN transform - node: {}, inputRows: {}", node.getName(), inputData.size());
        return inputData;
    }

    private List<Map<String, Object>> executeAggregate(PipelineNode node, List<Map<String, Object>> inputData) {
        log.info("AGGREGATE transform - node: {}, inputRows: {}", node.getName(), inputData.size());
        return inputData;
    }

    private List<Map<String, Object>> executeScript(PipelineNode node, List<Map<String, Object>> inputData) {
        log.info("SCRIPT transform - node: {}, inputRows: {}", node.getName(), inputData.size());
        return inputData;
    }
}
