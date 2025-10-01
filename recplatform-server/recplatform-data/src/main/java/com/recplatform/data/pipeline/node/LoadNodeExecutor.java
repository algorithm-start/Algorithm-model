package com.recplatform.data.pipeline.node;

import com.recplatform.data.pipeline.NodeSubType;
import com.recplatform.data.pipeline.dto.PipelineNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Executor for LOAD type nodes.
 */
@Slf4j
@Component
public class LoadNodeExecutor extends AbstractNodeExecutor {

    @Override
    public List<Map<String, Object>> execute(PipelineNode node, List<Map<String, Object>> inputData) {
        NodeSubType subType = node.getSubType();
        log.info("Executing load node: id={}, subType={}", node.getId(), subType);
        return switch (subType) {
            case DB_WRITE -> executeDbWrite(node, inputData);
            case API_PUSH -> executeApiPush(node, inputData);
            case FILE_EXPORT -> executeFileExport(node, inputData);
            default -> {
                log.warn("Unsupported load sub-type: {}", subType);
                yield inputData;
            }
        };
    }

    private List<Map<String, Object>> executeDbWrite(PipelineNode node, List<Map<String, Object>> inputData) {
        log.info("DB_WRITE load - node: {}, rows: {}", node.getName(), inputData.size());
        return inputData;
    }

    private List<Map<String, Object>> executeApiPush(PipelineNode node, List<Map<String, Object>> inputData) {
        log.info("API_PUSH load - node: {}, rows: {}", node.getName(), inputData.size());
        return inputData;
    }

    private List<Map<String, Object>> executeFileExport(PipelineNode node, List<Map<String, Object>> inputData) {
        log.info("FILE_EXPORT load - node: {}, rows: {}", node.getName(), inputData.size());
        return inputData;
    }
}
