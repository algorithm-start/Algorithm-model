package com.recplatform.iam.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.recplatform.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("workspace")
public class WorkspaceEntity extends BaseEntity {

    private String name;

    private String code;

    private String description;

    private Long ownerId;
}
