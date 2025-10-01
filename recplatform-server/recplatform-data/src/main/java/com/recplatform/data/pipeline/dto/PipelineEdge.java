package com.recplatform.data.pipeline.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * Pipeline DAG edge definition.
 */
@Data
public class PipelineEdge implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String source;
    private String target;
}
