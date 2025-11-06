package com.intellidesk.cognitia.ingestion.models.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Metadata for resource uploads")
public record ResourceMetadata(
    @Schema(description = "Resource name") String name,
    @Schema(description = "Resource description") String description
) {}
