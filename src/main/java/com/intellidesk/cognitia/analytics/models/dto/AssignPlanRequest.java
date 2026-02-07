package com.intellidesk.cognitia.analytics.models.dto;

import lombok.Data;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Request to assign a plan to a tenant")
public class AssignPlanRequest {
    
    @Schema(description = "Plan UUID to assign", required = true)
    private UUID planId;
    
    @Schema(description = "Reset usage counters when assigning the plan", defaultValue = "true")
    private boolean resetUsage = true;
    
    @Schema(description = "Order reference for payment verification. Required when upgrading to a higher-priced plan. " +
                          "The orderRef must reference a verified, unfulfilled payment order for this specific plan.")
    private String orderRef;
}
