package com.intellidesk.cognitia.userandauth.models.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Simple user DTO")
public record UserDTO(
    @Schema(description = "Name") String name,
    @Schema(description = "Email") String email,
    @Schema(description = "Phone number") String phoneNumber,
    @Schema(description = "Password (write-only)") String password) {}
