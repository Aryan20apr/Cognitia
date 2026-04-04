package com.intellidesk.cognitia.userandauth.models.dtos;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Department payload")
public record DepartmentDTO(
    @Schema(description = "Department id (set by server)") UUID id,
    @Schema(description = "Department name") String name,
    @Schema(description = "A short description for department") String description
) {}
