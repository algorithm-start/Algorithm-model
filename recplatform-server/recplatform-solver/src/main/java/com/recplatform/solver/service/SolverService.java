package com.recplatform.solver.service;

import com.recplatform.common.model.PageRequest;
import com.recplatform.common.model.PageResult;
import com.recplatform.solver.model.dto.AlgorithmInfo;
import com.recplatform.solver.model.dto.AlgorithmRecommendation;
import com.recplatform.solver.model.dto.ProblemDefinition;
import com.recplatform.solver.model.dto.ProblemSubmitRequest;
import com.recplatform.solver.model.dto.SolverConfig;
import com.recplatform.solver.model.vo.SolverJobVO;

import java.util.List;

/**
 * Service interface for solver operations.
 */
public interface SolverService {

    /**
     * Submit a new optimization problem.
     *
     * @param request the problem submission request
     * @return the created solver job view object
     */
    SolverJobVO submitProblem(ProblemSubmitRequest request);

    /**
     * Get a solver job by ID.
     *
     * @param id the job ID
     * @return the solver job view object
     */
    SolverJobVO getProblem(Long id);

    /**
     * Trigger solving for a submitted problem.
     *
     * @param id     the job ID
     * @param config the solver configuration
     * @return the updated solver job view object
     */
    SolverJobVO solveProblem(Long id, SolverConfig config);

    /**
     * List all available algorithms.
     *
     * @return list of algorithm information
     */
    List<AlgorithmInfo> listAlgorithms();

    /**
     * Auto-detect problem type and recommend algorithm.
     *
     * @param problemDefinition the problem definition to analyze
     * @return algorithm recommendation
     */
    AlgorithmRecommendation autoSelectAlgorithm(ProblemDefinition problemDefinition);

    /**
     * Update solver parameters for a job.
     *
     * @param id     the job ID
     * @param config the new solver configuration
     * @return the updated solver job view object
     */
    SolverJobVO updateParams(Long id, SolverConfig config);

    /**
     * Cancel or delete a solver job.
     *
     * @param id the job ID
     */
    void cancelProblem(Long id);

    /**
     * List all problems with pagination.
     *
     * @param pageRequest pagination parameters
     * @return paginated result of solver jobs
     */
    PageResult<SolverJobVO> listProblems(PageRequest pageRequest);
}
