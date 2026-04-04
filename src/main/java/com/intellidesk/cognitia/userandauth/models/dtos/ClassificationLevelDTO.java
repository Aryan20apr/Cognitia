package com.intellidesk.cognitia.userandauth.models.dtos;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Classification level payload")
public record ClassificationLevelDTO(
    @Schema(description = "Classification level id (set by server)") UUID id,
    @Schema(description = "Level name") String name,
    @Schema(description = "Rank (higher = more sensitive)") Integer rank
) {}
