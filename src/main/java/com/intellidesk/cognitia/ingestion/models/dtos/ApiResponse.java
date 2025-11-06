package com.intellidesk.cognitia.ingestion.models.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "Generic API response wrapper")
public record ApiResponse<T>(
        String message,
        boolean success,
        T data) {
}
