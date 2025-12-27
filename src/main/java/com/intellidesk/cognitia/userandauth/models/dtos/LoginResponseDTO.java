package com.intellidesk.cognitia.userandauth.models.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Schema(description = "Login response containing access token and user details")
public class LoginResponseDTO {
    
    @Schema(description = "JWT access token")
    String accessToken;
    
    @Schema(description = "Access token expiration timestamp (Unix timestamp in milliseconds)")
    Long expiresAt;
    
    @Schema(description = "Authenticated user details")
    UserDetailsDTO userDetailsDTO;
}
