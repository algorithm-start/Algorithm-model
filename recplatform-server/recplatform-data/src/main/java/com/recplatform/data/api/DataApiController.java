package com.recplatform.data.api;

import com.recplatform.common.result.Result;
import com.recplatform.data.api.dto.DataApiCreateRequest;
import com.recplatform.data.api.dto.DataApiVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Data API publishing controller.
 */
@RestController
@RequestMapping("/api/v1/data/apis")
@RequiredArgsConstructor
public class DataApiController {

    private final DataApiService dataApiService;

    @PostMapping
    public Result<DataApiVO> create(@Valid @RequestBody DataApiCreateRequest request) {
        return Result.success(dataApiService.create(request));
    }

    @GetMapping
    public Result<List<DataApiVO>> list() {
        return Result.success(dataApiService.list());
    }

    @GetMapping("/{id}")
    public Result<DataApiVO> get(@PathVariable Long id) {
        return Result.success(dataApiService.get(id));
    }

    @PutMapping("/{id}")
    public Result<DataApiVO> update(@PathVariable Long id,
                                    @Valid @RequestBody DataApiCreateRequest request) {
        return Result.success(dataApiService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        dataApiService.delete(id);
        return Result.success();
    }

    @PostMapping("/{id}/publish")
    public Result<DataApiVO> publish(@PathVariable Long id) {
        return Result.success(dataApiService.publish(id));
    }

    @PostMapping("/{id}/test")
    public Result<Map<String, Object>> test(@PathVariable Long id,
                                            @RequestBody(required = false) Map<String, Object> params) {
        return Result.success(dataApiService.test(id, params));
    }

    @GetMapping("/{id}/stats")
    public Result<Map<String, Object>> stats(@PathVariable Long id) {
        return Result.success(dataApiService.getStats(id));
    }
}
