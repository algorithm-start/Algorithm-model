package com.recplatform.solver.service;

import com.recplatform.common.enums.AlgorithmType;
import com.recplatform.solver.model.dto.AlgorithmInfo;
import com.recplatform.solver.model.dto.AlgorithmRecommendation;
import com.recplatform.solver.model.dto.ProblemDefinition;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Registry of available solver algorithms with metadata and recommendation logic.
 */
@Slf4j
@Component
public class AlgorithmRegistry {

    private final Map<AlgorithmType, AlgorithmInfo> registry = new LinkedHashMap<>();

    @PostConstruct
    public void init() {
        register(AlgorithmType.LINEAR_PROGRAMMING, "LP",
                "Linear Programming - continuous variables with linear constraints and objective",
                List.of("LP"), "100K+ variables",
                Map.of("method", List.of("simplex", "interior-point")));

        register(AlgorithmType.MIXED_INTEGER_LINEAR, "MILP",
                "Mixed Integer Linear Programming - integer/binary variables with linear constraints",
                List.of("LP", "MILP"), "10K+ variables",
                Map.of("method", List.of("branch-and-bound", "cutting-planes", "heuristics")));

        register(AlgorithmType.MIXED_INTEGER_QUADRATIC, "MIQP",
                "Mixed Integer Quadratic Programming - integer variables with quadratic objective",
                List.of("QP", "MIQP"), "1K+ variables",
                Map.of("method", List.of("branch-and-bound", "outer-approximation")));

        register(AlgorithmType.QUADRATIC_PROGRAMMING, "QP",
                "Quadratic Programming - continuous variables with quadratic objective",
                List.of("QP"), "10K+ variables",
                Map.of("method", List.of("interior-point", "active-set")));

        register(AlgorithmType.SIMPLEX, "SIMPLEX",
                "Simplex Method - classical algorithm for LP problems",
                List.of("LP"), "100K+ variables",
                Map.of("variant", List.of("primal", "dual", "network")));

        register(AlgorithmType.INTERIOR_POINT, "IPM",
                "Interior Point Method - polynomial-time algorithm for LP and QP",
                List.of("LP", "QP"), "100K+ variables",
                Map.of("variant", List.of("primal-dual", "predictor-corrector")));

        register(AlgorithmType.BRANCH_AND_BOUND, "BB",
                "Branch and Bound - exact algorithm for discrete optimization",
                List.of("MILP", "MIQP"), "1K+ variables",
                Map.of("strategy", List.of("depth-first", "best-first", "DFS")));

        register(AlgorithmType.SECOND_ORDER_CONE, "SOCP",
                "Second-Order Cone Programming - convex optimization with cone constraints",
                List.of("SOCP"), "10K+ variables",
                Map.of("method", List.of("interior-point")));

        register(AlgorithmType.MIXED_INTEGER_NONLINEAR, "MINLP",
                "Mixed Integer Nonlinear Programming - integer variables with nonlinear constraints",
                List.of("NLP", "MINLP"), "500+ variables",
                Map.of("method", List.of("branch-and-bound", "outer-approximation", "penalty")));

        register(AlgorithmType.HEURISTIC_GA, "GA",
                "Genetic Algorithm - metaheuristic for complex optimization",
                List.of("NLP", "MINLP", "combinatorial"), "10K+ variables",
                Map.of("population_size", "100", "mutation_rate", "0.01", "crossover_rate", "0.9"));

        register(AlgorithmType.HEURISTIC_SA, "SA",
                "Simulated Annealing - metaheuristic inspired by annealing",
                List.of("NLP", "MINLP", "combinatorial"), "10K+ variables",
                Map.of("initial_temperature", "1000", "cooling_rate", "0.95"));

        register(AlgorithmType.HEURISTIC_PSO, "PSO",
                "Particle Swarm Optimization - swarm intelligence metaheuristic",
                List.of("NLP", "continuous"), "10K+ variables",
                Map.of("swarm_size", "50", "inertia", "0.7", "c1", "1.5", "c2", "1.5"));

        register(AlgorithmType.HEURISTIC_ACO, "ACO",
                "Ant Colony Optimization - metaheuristic for combinatorial problems",
                List.of("combinatorial", "TSP", "routing"), "1K+ variables",
                Map.of("colony_size", "50", "evaporation_rate", "0.1", "alpha", "1.0", "beta", "2.0"));

        log.info("AlgorithmRegistry initialized with {} algorithms", registry.size());
    }

    private void register(AlgorithmType type, String code, String description,
                          List<String> supportedProblemTypes, String maxScale,
                          Map<String, Object> parameters) {
        AlgorithmInfo info = new AlgorithmInfo();
        info.setType(type);
        info.setCode(code);
        info.setDescription(description);
        info.setSupportedProblemTypes(supportedProblemTypes);
        info.setMaxScale(maxScale);
        info.setParameters(parameters);
        registry.put(type, info);
    }

    /**
     * Get all registered algorithms.
     *
     * @return unmodifiable list of algorithm information
     */
    public List<AlgorithmInfo> getAlgorithms() {
        return Collections.unmodifiableList(new ArrayList<>(registry.values()));
    }

    /**
     * Get algorithm info by type.
     *
     * @param type the algorithm type
     * @return algorithm info, or null if not found
     */
    public AlgorithmInfo getAlgorithm(AlgorithmType type) {
        return registry.get(type);
    }

    /**
     * Recommend algorithms based on problem characteristics.
     *
     * @param problemDefinition the problem to analyze
     * @return algorithm recommendation
     */
    public AlgorithmRecommendation recommend(ProblemDefinition problemDefinition) {
        if (problemDefinition == null) {
            return buildRecommendation(AlgorithmType.LINEAR_PROGRAMMING, 0.0,
                    Collections.emptyList(), "No problem definition provided");
        }

        boolean hasInteger = hasIntegerVariables(problemDefinition);
        boolean hasQuadratic = hasQuadraticObjective(problemDefinition);
        boolean hasNonlinear = hasNonlinearExpressions(problemDefinition);
        int scale = estimateScale(problemDefinition);

        AlgorithmType recommended;
        double confidence;
        String reasoning;
        List<AlgorithmType> alternatives = new ArrayList<>();

        if (hasNonlinear) {
            recommended = AlgorithmType.MIXED_INTEGER_NONLINEAR;
            confidence = 0.7;
            reasoning = "Nonlinear expressions detected";
            alternatives.add(AlgorithmType.HEURISTIC_GA);
            alternatives.add(AlgorithmType.HEURISTIC_SA);
        } else if (hasQuadratic && hasInteger) {
            recommended = AlgorithmType.MIXED_INTEGER_QUADRATIC;
            confidence = 0.85;
            reasoning = "Quadratic objective with integer variables detected";
            alternatives.add(AlgorithmType.BRANCH_AND_BOUND);
            alternatives.add(AlgorithmType.MIXED_INTEGER_LINEAR);
        } else if (hasQuadratic) {
            recommended = AlgorithmType.QUADRATIC_PROGRAMMING;
            confidence = 0.9;
            reasoning = "Quadratic objective with continuous variables detected";
            alternatives.add(AlgorithmType.INTERIOR_POINT);
            alternatives.add(AlgorithmType.LINEAR_PROGRAMMING);
        } else if (hasInteger) {
            recommended = scale > 1000 ? AlgorithmType.MIXED_INTEGER_LINEAR : AlgorithmType.BRANCH_AND_BOUND;
            confidence = 0.85;
            reasoning = "Integer/binary variables with linear structure detected";
            alternatives.add(AlgorithmType.MIXED_INTEGER_LINEAR);
            alternatives.add(AlgorithmType.BRANCH_AND_BOUND);
            alternatives.add(AlgorithmType.HEURISTIC_GA);
        } else {
            recommended = scale > 10000 ? AlgorithmType.INTERIOR_POINT : AlgorithmType.SIMPLEX;
            confidence = 0.95;
            reasoning = "Pure linear programming problem detected";
            alternatives.add(AlgorithmType.LINEAR_PROGRAMMING);
            alternatives.add(AlgorithmType.INTERIOR_POINT);
        }

        return buildRecommendation(recommended, confidence, alternatives, reasoning);
    }

    private boolean hasIntegerVariables(ProblemDefinition pd) {
        if (pd.getVariables() == null) {
            return false;
        }
        return pd.getVariables().stream()
                .anyMatch(v -> "INTEGER".equalsIgnoreCase(v.getType())
                        || "BINARY".equalsIgnoreCase(v.getType()));
    }

    private boolean hasQuadraticObjective(ProblemDefinition pd) {
        if (pd.getObjective() == null || pd.getObjective().getExpression() == null) {
            return false;
        }
        String expr = pd.getObjective().getExpression();
        // Heuristic: quadratic if expression contains squared terms or product of two variables
        return expr.contains("**2") || expr.contains("^2") || expr.contains("*");
    }

    private boolean hasNonlinearExpressions(ProblemDefinition pd) {
        if (pd.getConstraints() == null) {
            return false;
        }
        // Heuristic: check for common nonlinear patterns in constraint expressions
        return pd.getConstraints().stream()
                .anyMatch(c -> c.getExpression() != null &&
                        (c.getExpression().contains("sin") || c.getExpression().contains("cos")
                                || c.getExpression().contains("exp") || c.getExpression().contains("log")
                                || c.getExpression().contains("sqrt") || c.getExpression().contains("**")
                                || c.getExpression().contains("^")));
    }

    private int estimateScale(ProblemDefinition pd) {
        int vars = pd.getVariables() != null ? pd.getVariables().size() : 0;
        int cons = pd.getConstraints() != null ? pd.getConstraints().size() : 0;
        return vars + cons;
    }

    private AlgorithmRecommendation buildRecommendation(AlgorithmType recommended, double confidence,
                                                         List<AlgorithmType> alternatives, String reasoning) {
        AlgorithmRecommendation rec = new AlgorithmRecommendation();
        rec.setRecommended(recommended);
        rec.setConfidence(confidence);
        rec.setAlternatives(alternatives);
        rec.setReasoning(reasoning);
        return rec;
    }
}
