package com.recplatform.data.query.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * Column metadata DTO.
 */
@Data
@Builder
public class ColumnMeta implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private String type;
    private Boolean nullable;
}
