package com.recplatform.iam.dto;

import lombok.Data;

@Data
public class WorkspaceCreateRequest {

    private String name;

    private String code;

    private String description;
}
