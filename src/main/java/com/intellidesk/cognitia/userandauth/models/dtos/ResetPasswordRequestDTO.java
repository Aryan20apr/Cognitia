package com.intellidesk.cognitia.userandauth.models.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequestDTO {
    @NotBlank @Email
    private String email;

    @NotBlank
    private String otp;

    @NotBlank @Size(min = 8)
    private String newPassword;
}
