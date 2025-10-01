package com.recplatform.solver.service;

import com.recplatform.solver.model.vo.SolverExampleDetailVO;
import com.recplatform.solver.model.vo.SolverExampleSummaryVO;

import java.util.List;

/**
 * Service for built-in solver example problems.
 */
public interface SolverExampleService {

    /**
     * List all example problems (summary only).
     */
    List<SolverExampleSummaryVO> listExamples();

    /**
     * Get the full detail of a single example by its business key.
     */
    SolverExampleDetailVO getExample(String exampleKey);
}
