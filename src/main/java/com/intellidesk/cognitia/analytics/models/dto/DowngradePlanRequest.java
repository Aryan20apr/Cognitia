package com.intellidesk.cognitia.analytics.models.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Request to downgrade to a lower-priced plan")
public class DowngradePlanRequest {

    @NotNull
    @Schema(description = "Target plan UUID to downgrade to", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID planId;
}
