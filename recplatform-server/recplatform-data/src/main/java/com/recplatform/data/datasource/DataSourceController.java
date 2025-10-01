package com.recplatform.data.datasource;

import com.recplatform.common.model.PageRequest;
import com.recplatform.common.model.PageResult;
import com.recplatform.common.result.Result;
import com.recplatform.data.datasource.dto.ConnectionTestResult;
import com.recplatform.data.datasource.dto.DataSourceCreateRequest;
import com.recplatform.data.datasource.dto.DataSourceVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Data source management controller.
 */
@RestController
@RequestMapping("/api/v1/data/sources")
@RequiredArgsConstructor
public class DataSourceController {

    private final DataSourceService dataSourceService;

    @PostMapping
    public Result<DataSourceVO> create(@Valid @RequestBody DataSourceCreateRequest request) {
        return Result.success(dataSourceService.create(request));
    }

    @GetMapping
    public Result<PageResult<DataSourceVO>> list(PageRequest pageRequest) {
        return Result.success(dataSourceService.list(pageRequest));
    }

    @GetMapping("/{id}")
    public Result<DataSourceVO> get(@PathVariable Long id) {
        return Result.success(dataSourceService.get(id));
    }

    @PutMapping("/{id}")
    public Result<DataSourceVO> update(@PathVariable Long id,
                                       @Valid @RequestBody DataSourceCreateRequest request) {
        return Result.success(dataSourceService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        dataSourceService.delete(id);
        return Result.success();
    }

    @PostMapping("/{id}/test")
    public Result<ConnectionTestResult> testConnection(@PathVariable Long id) {
        return Result.success(dataSourceService.testConnection(id));
    }

    @PostMapping("/test")
    public Result<ConnectionTestResult> testConnection(@Valid @RequestBody DataSourceCreateRequest request) {
        return Result.success(dataSourceService.testConnection(request));
    }

    @GetMapping("/{id}/metadata")
    public Result<Map<String, Object>> getMetadata(@PathVariable Long id) {
        return Result.success(dataSourceService.getMetadata(id));
    }
}
