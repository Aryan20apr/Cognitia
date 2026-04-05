package com.intellidesk.cognitia.userandauth.models.dtos;

import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Role creation payload")
public class RoleCreationDTO {

    @Schema(description = "Role id (set by server)", accessMode = Schema.AccessMode.READ_ONLY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    Integer roleId;

    @Schema(description = "Name of role")
    String name;

    @Schema(description = "Permissions to attach")
    Set<PermissionDTO> permissions;

    @Schema(description = "Classification level ID that defines the max clearance for this role")
    UUID clearanceLevelId;
    
    @Schema(description = "Resolved clearance rank", accessMode = Schema.AccessMode.READ_ONLY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    Integer clearanceRank;
}
