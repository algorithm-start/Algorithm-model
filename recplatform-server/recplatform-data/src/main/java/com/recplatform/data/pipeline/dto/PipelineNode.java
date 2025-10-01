package com.recplatform.data.pipeline.dto;

import com.recplatform.data.pipeline.NodeSubType;
import com.recplatform.data.pipeline.NodeType;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * Pipeline DAG node definition.
 */
@Data
public class PipelineNode implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private NodeType type;
    private NodeSubType subType;
    private String name;
    private Map<String, Object> config;

    /**
     * Canvas coordinates ({@code x}, {@code y}) so the editor can restore the
     * node layout. Persisted as part of the definition JSON.
     */
    private Map<String, Object> position;
}
