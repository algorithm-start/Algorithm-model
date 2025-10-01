package com.recplatform.audit.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Alert trigger entity.
 */
@Data
@TableName("audit_alert_trigger")
public class AlertTriggerEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long ruleId;

    private String ruleName;

    private String severity;

    /**
     * JSON - list of matched log IDs
     */
    private String matchedEvents;

    private String message;

    private Boolean acknowledged;

    private LocalDateTime createTime;
}
