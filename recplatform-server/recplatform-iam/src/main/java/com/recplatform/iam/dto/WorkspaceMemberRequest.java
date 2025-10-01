package com.recplatform.iam.dto;

import lombok.Data;

import java.util.List;

@Data
public class WorkspaceMemberRequest {

    private List<Long> userIds;

    /**
     * ADMIN or MEMBER (default: MEMBER)
     */
    private String role;
}
