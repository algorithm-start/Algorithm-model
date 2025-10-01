package com.recplatform.audit.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Alert trigger view object.
 */
@Data
public class AlertVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long ruleId;

    private String ruleName;

    private String severity;

    private String matchedEvents;

    private String message;

    private Boolean acknowledged;

    private LocalDateTime createTime;
}
