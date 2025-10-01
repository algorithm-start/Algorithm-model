package com.recplatform.solver.controller;

import com.recplatform.common.result.Result;
import com.recplatform.solver.model.vo.SolverExampleDetailVO;
import com.recplatform.solver.model.vo.SolverExampleSummaryVO;
import com.recplatform.solver.service.SolverExampleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for built-in solver example problems.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/solver/examples")
@RequiredArgsConstructor
public class SolverExampleController {

    private final SolverExampleService solverExampleService;

    /**
     * List all example problems (summary only).
     */
    @GetMapping
    public Result<List<SolverExampleSummaryVO>> listExamples() {
        return Result.success(solverExampleService.listExamples());
    }

    /**
     * Get full detail of a single example by its business key.
     */
    @GetMapping("/{exampleKey}")
    public Result<SolverExampleDetailVO> getExample(@PathVariable String exampleKey) {
        return Result.success(solverExampleService.getExample(exampleKey));
    }
}
