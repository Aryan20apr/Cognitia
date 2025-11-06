package com.intellidesk.cognitia.userandauth.models.dtos;

import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Login request payload")
public class LoginRequestDTO {
    @Schema(description = "User email", example = "user@example.com")
    String email;
    @Schema(description = "User password", example = "secret")
    String password;
    @Schema(description = "Client device id")
    String deviceId;
    @Schema(description = "Client IP")
    String ip;
    @Schema(description = "User agent string")
    String userAgent;
}
