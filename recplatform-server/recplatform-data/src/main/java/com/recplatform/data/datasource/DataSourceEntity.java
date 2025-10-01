package com.recplatform.data.datasource;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.recplatform.common.enums.DataSourceType;
import com.recplatform.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Data source entity.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("data_source")
public class DataSourceEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableField(value = "workspace_id", fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private Long workspaceId;

    private String name;

    private String description;

    private DataSourceType type;

    private String host;

    private Integer port;

    @TableField("database_name")
    private String database;

    private String username;

    private String password;

    private String options;

    private DataSourceStatus status;

    private LocalDateTime lastTestTime;

    private String lastTestResult;
}
