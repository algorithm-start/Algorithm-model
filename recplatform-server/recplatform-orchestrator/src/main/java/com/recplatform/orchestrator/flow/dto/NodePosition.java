package com.recplatform.orchestrator.flow.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * Node position for visual editor.
 */
@Data
public class NodePosition implements Serializable {

    private static final long serialVersionUID = 1L;

    private double x;
    private double y;
}
