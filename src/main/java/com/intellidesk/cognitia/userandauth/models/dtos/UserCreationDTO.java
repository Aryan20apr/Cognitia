package com.intellidesk.cognitia.userandauth.models.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request payload to create a user")
public record UserCreationDTO(
    @Schema(description = "User name") String name,
    @Schema(description = "Password") String password,
    @Schema(description = "Email") String email,
    @Schema(description = "Company / tenant id") String companyId,
    @Schema(description = "Phone number") String phoneNumber,
    @Schema(description = "Role payload") RoleCreationDTO roleDetails ) {
    
}
