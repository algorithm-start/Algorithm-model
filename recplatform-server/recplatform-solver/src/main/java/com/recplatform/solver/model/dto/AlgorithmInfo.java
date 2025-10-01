package com.recplatform.solver.model.dto;

import com.recplatform.common.enums.AlgorithmType;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Information about an available solver algorithm.
 */
@Data
public class AlgorithmInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private AlgorithmType type;

    private String code;

    private String description;

    private List<String> supportedProblemTypes;

    private String maxScale;

    private Map<String, Object> parameters;
}
