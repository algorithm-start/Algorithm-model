package com.recplatform.solver.model.dto;

import com.recplatform.common.enums.AlgorithmType;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Algorithm recommendation returned by auto-select.
 */
@Data
public class AlgorithmRecommendation implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The recommended algorithm type.
     */
    private AlgorithmType recommended;

    /**
     * Confidence score (0.0 - 1.0).
     */
    private Double confidence;

    /**
     * Alternative algorithm types ranked by suitability.
     */
    private List<AlgorithmType> alternatives;

    /**
     * Explanation for the recommendation.
     */
    private String reasoning;
}
