package com.recplatform.orchestrator.service;

import com.recplatform.common.model.PageRequest;
import com.recplatform.common.model.PageResult;
import com.recplatform.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for service registry management.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/orchestrator/services")
@RequiredArgsConstructor
public class ServiceRegistryController {

    private final ServiceRegistryService serviceRegistryService;

    @PostMapping
    public Result<RegisteredServiceEntity> registerService(@RequestBody RegisteredServiceEntity entity) {
        return Result.success(serviceRegistryService.registerService(entity));
    }

    @GetMapping
    public Result<PageResult<RegisteredServiceEntity>> listServices(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status) {
        PageRequest pageRequest = new PageRequest();
        pageRequest.setPage(page);
        pageRequest.setSize(size);
        return Result.success(serviceRegistryService.listServices(pageRequest, type, status));
    }

    @GetMapping("/{id}")
    public Result<RegisteredServiceEntity> getService(@PathVariable Long id) {
        return Result.success(serviceRegistryService.getService(id));
    }

    @PutMapping("/{id}")
    public Result<RegisteredServiceEntity> updateService(@PathVariable Long id,
                                                          @RequestBody RegisteredServiceEntity updates) {
        return Result.success(serviceRegistryService.updateService(id, updates));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteService(@PathVariable Long id) {
        serviceRegistryService.deleteService(id);
        return Result.success();
    }

    @PostMapping("/{id}/test")
    public Result<Map<String, Object>> testService(@PathVariable Long id) {
        return Result.success(serviceRegistryService.testService(id));
    }
}
