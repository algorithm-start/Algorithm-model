package com.recplatform.solver.controller;

import com.recplatform.common.model.PageRequest;
import com.recplatform.common.model.PageResult;
import com.recplatform.common.result.Result;
import com.recplatform.solver.model.dto.AlgorithmInfo;
import com.recplatform.solver.model.dto.AlgorithmRecommendation;
import com.recplatform.solver.model.dto.ProblemDefinition;
import com.recplatform.solver.model.dto.ProblemSubmitRequest;
import com.recplatform.solver.model.dto.SolverConfig;
import com.recplatform.solver.model.vo.SolverJobVO;
import com.recplatform.solver.service.SolverService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for solver operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/solver")
@RequiredArgsConstructor
public class SolverController {

    private final SolverService solverService;

    /**
     * Submit a new optimization problem.
     */
    @PostMapping("/problems")
    public Result<SolverJobVO> submitProblem(@Valid @RequestBody ProblemSubmitRequest request) {
        log.info("Received problem submission: {}", request.getProblemName());
        SolverJobVO job = solverService.submitProblem(request);
        return Result.success(job);
    }

    /**
     * Get problem status and result by ID.
     */
    @GetMapping("/problems/{id}")
    public Result<SolverJobVO> getProblem(@PathVariable Long id) {
        SolverJobVO job = solverService.getProblem(id);
        return Result.success(job);
    }

    /**
     * Trigger solving for a submitted problem.
     */
    @PostMapping("/problems/{id}/solve")
    public Result<SolverJobVO> solveProblem(@PathVariable Long id, @RequestBody SolverConfig config) {
        log.info("Triggering solve for problem id={}", id);
        SolverJobVO job = solverService.solveProblem(id, config);
        return Result.success(job);
    }

    /**
     * List all available algorithms.
     */
    @GetMapping("/algorithms")
    public Result<List<AlgorithmInfo>> listAlgorithms() {
        List<AlgorithmInfo> algorithms = solverService.listAlgorithms();
        return Result.success(algorithms);
    }

    /**
     * Auto-detect problem type and recommend algorithm.
     */
    @PostMapping("/auto-select")
    public Result<AlgorithmRecommendation> autoSelectAlgorithm(@RequestBody ProblemDefinition problemDefinition) {
        log.info("Auto-selecting algorithm for problem");
        AlgorithmRecommendation recommendation = solverService.autoSelectAlgorithm(problemDefinition);
        return Result.success(recommendation);
    }

    /**
     * Update solver parameters for a problem.
     */
    @PutMapping("/problems/{id}/params")
    public Result<SolverJobVO> updateParams(@PathVariable Long id, @RequestBody SolverConfig config) {
        log.info("Updating params for problem id={}", id);
        SolverJobVO job = solverService.updateParams(id, config);
        return Result.success(job);
    }

    /**
     * Cancel or delete a problem.
     */
    @DeleteMapping("/problems/{id}")
    public Result<Void> cancelProblem(@PathVariable Long id) {
        log.info("Cancelling problem id={}", id);
        solverService.cancelProblem(id);
        return Result.success();
    }

    /**
     * List all problems with pagination.
     */
    @GetMapping("/problems")
    public Result<PageResult<SolverJobVO>> listProblems(PageRequest pageRequest) {
        PageResult<SolverJobVO> page = solverService.listProblems(pageRequest);
        return Result.success(page);
    }
}
