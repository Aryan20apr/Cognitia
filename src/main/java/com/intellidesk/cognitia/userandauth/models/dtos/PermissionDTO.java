package com.intellidesk.cognitia.userandauth.models.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Schema(description = "Permission DTO")
public class PermissionDTO {
    
    @Schema(description = "Permission numeric id")
    Integer id;
    @Schema(description = "Permission name")
    String name;
}
