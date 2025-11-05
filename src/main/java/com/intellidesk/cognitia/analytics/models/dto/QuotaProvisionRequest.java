package com.intellidesk.cognitia.analytics.models.dto;

import com.intellidesk.cognitia.analytics.models.enums.EnforcementMode;
import lombok.Data;

@Data
public class QuotaProvisionRequest {
    private Long maxPromptTokens;
    private Long maxCompletionTokens;
    private Long maxTotalTokens;
    private Integer maxResources;
    private Integer maxUsers;
    private EnforcementMode enforcementMode;
}