package com.intellidesk.cognitia.ingestion.models.dtos;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Metadata for resource uploads")
public record ResourceMetadata(
    @Schema(description = "Resource name") String name,
    @Schema(description = "Resource description") String description,
    @Schema(description = "Department ID (optional, LLM classifies if omitted)") UUID departmentId,
    @Schema(description = "Classification level ID (optional, LLM classifies if omitted)") UUID classificationLevelId
) {}
