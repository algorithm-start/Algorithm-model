package com.recplatform.audit.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.recplatform.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Alert rule entity.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("audit_alert_rule")
public class AlertRuleEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String name;

    private String description;

    /**
     * JSON condition, e.g., {"action":"DELETE","frequency":">5","timeWindow":"1h"}
     * Column quoted because "condition" is a SQL reserved keyword.
     */
    @TableField("\"condition\"")
    private String condition;

    /**
     * Severity: LOW, MEDIUM, HIGH, CRITICAL
     */
    private String severity;

    private Boolean enabled;

    /**
     * JSON notification config - email/webhook
     */
    private String notification;
}
