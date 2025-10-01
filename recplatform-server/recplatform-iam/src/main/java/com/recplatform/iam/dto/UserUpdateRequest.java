package com.recplatform.iam.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * User update request DTO.
 */
@Data
public class UserUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String nickname;

    private String email;

    private String phone;

    private String status;
}
