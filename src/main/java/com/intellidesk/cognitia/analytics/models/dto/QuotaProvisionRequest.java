package com.intellidesk.cognitia.analytics.models.dto;

import com.intellidesk.cognitia.analytics.models.enums.EnforcementMode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Provision custom tenant quota")
public class QuotaProvisionRequest {
    private Long maxPromptTokens;
    private Long maxCompletionTokens;
    private Long maxTotalTokens;
    private Integer maxResources;
    private Integer maxUsers;
    private EnforcementMode enforcementMode;
}