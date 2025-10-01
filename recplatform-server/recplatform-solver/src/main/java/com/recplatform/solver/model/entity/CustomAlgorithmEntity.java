package com.recplatform.solver.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.recplatform.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("solver_custom_algorithm")
public class CustomAlgorithmEntity extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String name;

    private String code;

    private String category;

    private String description;

    @TableField("problem_types")
    private String problemTypes;

    @TableField("integration_type")
    private String integrationType;

    @TableField("api_endpoint")
    private String apiEndpoint;

    @TableField("script_content")
    private String scriptContent;

    private String params;

    private Integer enabled;

    @TableField(value = "workspace_id", fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private Long workspaceId;
}
