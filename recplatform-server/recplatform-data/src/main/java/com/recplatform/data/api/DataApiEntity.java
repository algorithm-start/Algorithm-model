package com.recplatform.data.api;

import com.baomidou.mybatisplus.annotation.TableName;
import com.recplatform.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Data API entity.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("data_api")
public class DataApiEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @com.baomidou.mybatisplus.annotation.TableField(value = "workspace_id", fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private Long workspaceId;

    private String name;
    private String description;
    private String path;
    private String method;
    private Long dataSourceId;
    private String query;
    private String parameters;
    private String responseMapping;
    private DataApiStatus status;
    private Boolean authRequired;
    private Integer rateLimit;
    private Long callCount;
}
