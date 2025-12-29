package com.intellidesk.cognitia.userandauth.models.dtos;

import java.util.Set;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Role creation payload")
public class RoleCreationDTO {

    @Schema(description = "Role id (set by server)")
    Integer roleId;
    @Schema(description = "Name of role")
    String name;
    @Schema(description = "Permissions to attach")
    Set<PermissionDTO> permissions; 
}
