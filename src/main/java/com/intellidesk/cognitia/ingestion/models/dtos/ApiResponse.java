package com.intellidesk.cognitia.ingestion.models.dtos;

import lombok.Builder;

@Builder
public record ApiResponse<T>(
        String message,
        boolean success,
        T data) {
}
