package com.intellidesk.cognitia.analytics.models.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class AssignPlanRequest {
    private UUID planId;
    private boolean resetUsage = true;
}
