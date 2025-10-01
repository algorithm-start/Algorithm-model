package com.recplatform.iam.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.recplatform.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("workspace_member")
public class WorkspaceMemberEntity extends BaseEntity {

    private Long workspaceId;

    private Long userId;

    /**
     * ADMIN or MEMBER
     */
    private String role;
}
