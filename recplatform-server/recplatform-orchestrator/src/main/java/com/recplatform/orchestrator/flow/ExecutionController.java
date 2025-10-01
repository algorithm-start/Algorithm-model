package com.recplatform.orchestrator.flow;

import com.recplatform.common.model.PageRequest;
import com.recplatform.common.model.PageResult;
import com.recplatform.common.result.Result;
import com.recplatform.orchestrator.flow.vo.FlowExecutionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for global flow execution records, decoupled from a specific
 * flow. The frontend "execution records" page consumes these endpoints under
 * /api/v1/orchestrator/executions.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/orchestrator/executions")
@RequiredArgsConstructor
public class ExecutionController {

    private final FlowService flowService;

    @GetMapping
    public Result<PageResult<FlowExecutionVO>> listExecutions(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String status) {
        PageRequest pageRequest = new PageRequest();
        pageRequest.setPage(page);
        pageRequest.setSize(size);
        return Result.success(flowService.listAllExecutions(pageRequest, status));
    }

    @GetMapping("/{executionId}")
    public Result<FlowExecutionVO> getExecution(@PathVariable Long executionId) {
        return Result.success(flowService.getExecution(executionId));
    }

    @PostMapping("/{executionId}/cancel")
    public Result<Void> cancelExecution(@PathVariable Long executionId) {
        flowService.stopFlow(executionId);
        return Result.success();
    }
}
