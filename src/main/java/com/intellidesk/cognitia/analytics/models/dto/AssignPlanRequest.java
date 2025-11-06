package com.intellidesk.cognitia.analytics.models.dto;

import lombok.Data;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Assign plan request")
public class AssignPlanRequest {
    @Schema(description = "Plan UUID")
    private UUID planId;
    @Schema(description = "Reset usage when assigning")
    private boolean resetUsage = true;
}
