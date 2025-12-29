package com.intellidesk.cognitia.userandauth.models.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload to create a user")
public record UserCreationDTO(

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    @Schema(description = "User name", example = "Davey Jones")
    String name,

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Schema(description = "Password", example = "StrongPass@123")
    String password,

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Schema(description = "Email", example = "davey@acme.com")
    String email,

    @NotBlank(message = "Company / tenant id is required")
    @Schema(description = "Company / tenant id", example = "bb70b62e-5179-4f50-bc16-d7750bf1de7a")
    String companyId,

    @NotBlank(message = "Phone number is required")
    @Pattern(
        regexp = "^[6-9]\\d{9}$",
        message = "Phone number must be a valid 10-digit Indian mobile number"
    )
    @Schema(description = "Phone number", example = "9876543210")
    String phoneNumber,

    @NotNull(message = "Role details are required")
    @Valid
    @Schema(description = "Role payload")
    RoleCreationDTO roleDetails

) { }