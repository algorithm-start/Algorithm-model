package com.recplatform.data.query;

import com.recplatform.common.result.Result;
import com.recplatform.data.query.dto.QueryRequest;
import com.recplatform.data.query.dto.QueryResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * OLAP query controller.
 */
@RestController
@RequestMapping("/api/v1/data/query")
@RequiredArgsConstructor
public class QueryController {

    private final QueryService queryService;

    @PostMapping
    public Result<QueryResult> execute(@Valid @RequestBody QueryRequest request) {
        return Result.success(queryService.executeQuery(request));
    }

    @PostMapping("/preview")
    public Result<QueryResult> preview(@Valid @RequestBody QueryRequest request) {
        return Result.success(queryService.preview(request));
    }
}
