package com.recplatform.data.pipeline.dto;

import com.recplatform.data.pipeline.PipelineMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * Pipeline create/update request DTO.
 */
@Data
public class PipelineCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Name must not be blank")
    private String name;
    private String description;
    @NotNull(message = "Definition must not be null")
    private PipelineDefinition definition;
    private PipelineMode mode;
    private String schedule;
}
