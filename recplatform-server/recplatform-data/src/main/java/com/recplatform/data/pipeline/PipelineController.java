package com.recplatform.data.pipeline;

import com.recplatform.common.model.PageRequest;
import com.recplatform.common.model.PageResult;
import com.recplatform.common.result.Result;
import com.recplatform.data.pipeline.dto.PipelineCreateRequest;
import com.recplatform.data.pipeline.dto.PipelineExecutionVO;
import com.recplatform.data.pipeline.dto.PipelineVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Pipeline management controller.
 */
@RestController
@RequestMapping("/api/v1/data/pipelines")
@RequiredArgsConstructor
public class PipelineController {

    private final PipelineService pipelineService;

    @PostMapping
    public Result<PipelineVO> create(@Valid @RequestBody PipelineCreateRequest request) {
        return Result.success(pipelineService.create(request));
    }

    @GetMapping
    public Result<PageResult<PipelineVO>> list(PageRequest pageRequest) {
        return Result.success(pipelineService.list(pageRequest));
    }

    @GetMapping("/{id}")
    public Result<PipelineVO> get(@PathVariable Long id) {
        return Result.success(pipelineService.get(id));
    }

    @PutMapping("/{id}")
    public Result<PipelineVO> update(@PathVariable Long id,
                                     @Valid @RequestBody PipelineCreateRequest request) {
        return Result.success(pipelineService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        pipelineService.delete(id);
        return Result.success();
    }

    @PostMapping("/{id}/run")
    public Result<PipelineExecutionVO> run(@PathVariable Long id) {
        return Result.success(pipelineService.executePipeline(id));
    }

    @PostMapping("/{id}/stop")
    public Result<Void> stop(@PathVariable Long id,
                             @RequestParam Long executionId) {
        pipelineService.stopPipeline(executionId);
        return Result.success();
    }

    @GetMapping("/{id}/status")
    public Result<PipelineExecutionVO> status(@PathVariable Long id,
                                              @RequestParam Long executionId) {
        return Result.success(pipelineService.getStatus(executionId));
    }

    @GetMapping("/{id}/history")
    public Result<List<PipelineExecutionVO>> history(@PathVariable Long id) {
        return Result.success(pipelineService.getHistory(id));
    }
}
