package com.intellidesk.cognitia.userandauth.models.dtos;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "User details exposed to clients")
public class UserDetailsDTO{
    
    @Schema(description = "User id")
    String userId;
    @Schema(description = "User name")
    String name; 
    @Schema(description = "User email")
    String email; 
    @Schema(description = "Phone number")
    String phoneNumber; 
    @Schema(description = "Role")
    String role; 
    @Schema(description = "Company id")
    String companyId; 
    @Schema(description = "Role display (duplicate field)")
    String Role; 
    @Schema(description = "Granted permissions")
    List<String> permissions;  
    
}
