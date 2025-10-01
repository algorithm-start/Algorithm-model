package com.recplatform.orchestrator.service.model;

import com.recplatform.common.model.PageRequest;
import com.recplatform.common.model.PageResult;
import com.recplatform.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for model registry management.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/orchestrator/models")
@RequiredArgsConstructor
public class ModelRegistryController {

    private final ModelRegistryService modelRegistryService;

    @PostMapping
    public Result<RegisteredModelEntity> registerModel(@RequestBody RegisteredModelEntity entity) {
        return Result.success(modelRegistryService.registerModel(entity));
    }

    @GetMapping
    public Result<PageResult<RegisteredModelEntity>> listModels(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String algorithmType,
            @RequestParam(required = false) String status) {
        PageRequest pageRequest = new PageRequest();
        pageRequest.setPage(page);
        pageRequest.setSize(size);
        return Result.success(modelRegistryService.listModels(pageRequest, algorithmType, status));
    }

    @GetMapping("/{id}")
    public Result<RegisteredModelEntity> getModel(@PathVariable Long id) {
        return Result.success(modelRegistryService.getModel(id));
    }

    @PutMapping("/{id}")
    public Result<RegisteredModelEntity> updateModel(@PathVariable Long id,
                                                      @RequestBody RegisteredModelEntity updates) {
        return Result.success(modelRegistryService.updateModel(id, updates));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteModel(@PathVariable Long id) {
        modelRegistryService.deleteModel(id);
        return Result.success();
    }
}
