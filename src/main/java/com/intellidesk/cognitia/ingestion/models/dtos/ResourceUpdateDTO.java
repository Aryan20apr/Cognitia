package com.intellidesk.cognitia.ingestion.models.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Resource rename/update payload")
public record ResourceUpdateDTO(
        @Size(min = 1, max = 255)
        String name,

        String description
) {}
