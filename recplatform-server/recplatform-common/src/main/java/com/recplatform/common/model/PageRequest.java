package com.recplatform.common.model;

import lombok.Data;

import java.io.Serializable;

/**
 * Pagination request parameters.
 */
@Data
public class PageRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer page = 1;

    private Integer size = 20;

    private String sortField;

    private String sortOrder;
}
