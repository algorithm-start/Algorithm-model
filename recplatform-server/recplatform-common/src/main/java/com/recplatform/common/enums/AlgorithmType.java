package com.recplatform.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Algorithm type enumeration for solver algorithms.
 */
@Getter
@AllArgsConstructor
public enum AlgorithmType {

    LINEAR_PROGRAMMING("LP", "Linear Programming"),
    MIXED_INTEGER_LINEAR("MILP", "Mixed Integer Linear Programming"),
    MIXED_INTEGER_NONLINEAR("MINLP", "Mixed Integer Nonlinear Programming"),
    QUADRATIC_PROGRAMMING("QP", "Quadratic Programming"),
    SECOND_ORDER_CONE("SOCP", "Second-Order Cone Programming"),
    MIXED_INTEGER_QUADRATIC("MIQP", "Mixed Integer Quadratic Programming"),
    SIMPLEX("SIMPLEX", "Simplex Method"),
    BRANCH_AND_BOUND("BB", "Branch and Bound"),
    INTERIOR_POINT("IPM", "Interior Point Method"),
    HEURISTIC_GA("GA", "Genetic Algorithm"),
    HEURISTIC_SA("SA", "Simulated Annealing"),
    HEURISTIC_PSO("PSO", "Particle Swarm Optimization"),
    HEURISTIC_ACO("ACO", "Ant Colony Optimization");

    private final String code;
    private final String description;
}
