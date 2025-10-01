package com.recplatform.orchestrator.flow;

import com.recplatform.common.model.PageRequest;
import com.recplatform.common.model.PageResult;
import com.recplatform.common.result.Result;
import com.recplatform.orchestrator.flow.dto.FlowDefinition;
import com.recplatform.orchestrator.flow.dto.FlowRunRequest;
import com.recplatform.orchestrator.flow.vo.FlowExecutionVO;
import com.recplatform.orchestrator.flow.vo.FlowVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for flow management and execution.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/orchestrator/flows")
@RequiredArgsConstructor
public class FlowController {

    private final FlowService flowService;

    @PostMapping
    public Result<FlowVO> createFlow(@RequestBody FlowCreateRequest request) {
        FlowVO vo = flowService.createFlow(
                request.getName(),
                request.getDescription(),
                request.getDefinition(),
                request.getCategory(),
                request.getTags()
        );
        return Result.success(vo);
    }

    @GetMapping
    public Result<PageResult<FlowVO>> listFlows(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status) {
        PageRequest pageRequest = new PageRequest();
        pageRequest.setPage(page);
        pageRequest.setSize(size);
        return Result.success(flowService.listFlows(pageRequest, category, status));
    }

    @GetMapping("/{id}")
    public Result<FlowVO> getFlow(@PathVariable Long id) {
        return Result.success(flowService.getFlow(id));
    }

    @PutMapping("/{id}")
    public Result<FlowVO> updateFlow(@PathVariable Long id, @RequestBody FlowCreateRequest request) {
        FlowVO vo = flowService.updateFlow(
                id,
                request.getName(),
                request.getDescription(),
                request.getDefinition(),
                request.getCategory(),
                request.getTags()
        );
        return Result.success(vo);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteFlow(@PathVariable Long id) {
        flowService.deleteFlow(id);
        return Result.success();
    }

    @PostMapping("/{id}/publish")
    public Result<FlowVO> publishFlow(@PathVariable Long id) {
        return Result.success(flowService.publishFlow(id));
    }

    @PostMapping({"/{id}/run", "/{id}/execute"})
    public Result<FlowExecutionVO> runFlow(@PathVariable Long id,
                                           @RequestBody(required = false) FlowRunRequest request) {
        Map<String, Object> inputParams = (request != null) ? request.getInputParams() : null;
        return Result.success(flowService.executeFlow(id, inputParams));
    }

    @PostMapping("/{id}/stop")
    public Result<Void> stopFlow(@PathVariable Long id,
                                 @RequestParam Long executionId) {
        flowService.stopFlow(executionId);
        return Result.success();
    }

    @GetMapping("/{id}/executions")
    public Result<PageResult<FlowExecutionVO>> listExecutions(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        PageRequest pageRequest = new PageRequest();
        pageRequest.setPage(page);
        pageRequest.setSize(size);
        return Result.success(flowService.listExecutions(id, pageRequest));
    }

    @GetMapping("/executions/{executionId}")
    public Result<FlowExecutionVO> getExecution(@PathVariable Long executionId) {
        return Result.success(flowService.getExecution(executionId));
    }

    /**
     * Request body for creating/updating a flow.
     */
    @lombok.Data
    public static class FlowCreateRequest {
        private String name;
        private String description;
        private FlowDefinition definition;
        private String category;
        private List<String> tags;
    }
}
