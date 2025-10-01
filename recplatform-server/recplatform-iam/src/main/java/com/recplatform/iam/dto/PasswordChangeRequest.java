package com.recplatform.iam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * Password change request DTO.
 */
@Data
public class PasswordChangeRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "New password cannot be blank")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    private String newPassword;
}
