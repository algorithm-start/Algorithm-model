package com.recplatform.data.pipeline.node;

import com.recplatform.data.pipeline.NodeSubType;
import com.recplatform.data.pipeline.dto.PipelineNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Executor for EXTRACT type nodes.
 */
@Slf4j
@Component
public class ExtractNodeExecutor extends AbstractNodeExecutor {

    @Override
    public List<Map<String, Object>> execute(PipelineNode node, List<Map<String, Object>> inputData) {
        NodeSubType subType = node.getSubType();
        log.info("Executing extract node: id={}, subType={}", node.getId(), subType);
        return switch (subType) {
            case DB_QUERY -> executeDbQuery(node);
            case API_CALL -> executeApiCall(node);
            case FILE_READ -> executeFileRead(node);
            default -> {
                log.warn("Unsupported extract sub-type: {}", subType);
                yield Collections.emptyList();
            }
        };
    }

    private List<Map<String, Object>> executeDbQuery(PipelineNode node) {
        log.info("DB_QUERY extraction - node: {}", node.getName());
        return new ArrayList<>();
    }

    private List<Map<String, Object>> executeApiCall(PipelineNode node) {
        log.info("API_CALL extraction - node: {}", node.getName());
        return new ArrayList<>();
    }

    private List<Map<String, Object>> executeFileRead(PipelineNode node) {
        log.info("FILE_READ extraction - node: {}", node.getName());
        return new ArrayList<>();
    }
}
