package com.recplatform.iam.dto;

import com.recplatform.iam.vo.UserVO;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * Login response DTO.
 */
@Data
@Builder
public class LoginResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String token;

    private String tokenType;

    private Long expiresIn;

    private UserVO user;

    public LoginResponse() {
        this.tokenType = "Bearer";
    }

    public LoginResponse(String token, String tokenType, Long expiresIn, UserVO user) {
        this.token = token;
        this.tokenType = "Bearer";
        this.expiresIn = expiresIn;
        this.user = user;
    }
}
