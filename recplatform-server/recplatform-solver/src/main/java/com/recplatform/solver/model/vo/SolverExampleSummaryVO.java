package com.recplatform.solver.model.vo;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * Summary view object for the example list (no full problem definition).
 */
@Data
@Builder
public class SolverExampleSummaryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    private String name;

    /**
     * Problem type: LP, MILP, QP, etc.
     */
    private String type;

    private String description;

    private String recommendedAlgorithm;

    private String difficulty;
}
