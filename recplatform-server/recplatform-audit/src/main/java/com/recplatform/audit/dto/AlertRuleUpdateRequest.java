package com.recplatform.audit.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * Alert rule update request DTO.
 */
@Data
public class AlertRuleUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;

    private String description;

    private String condition;

    private String severity;

    private String notification;
}
