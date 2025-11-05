package com.intellidesk.cognitia.analytics.models.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.intellidesk.cognitia.analytics.models.enums.EnforcementMode;
import com.intellidesk.cognitia.analytics.models.enums.QuotaStatus;

import lombok.Data;

@Data
public class TenantQuotaDTO {
    private UUID id;
    private UUID tenantId;
    private UUID planId;
    private String planName;
    private QuotaStatus status;
    private EnforcementMode enforcementMode;

    private LocalDate billingCycleStart;
    private LocalDate billingCycleEnd;

    private Long maxPromptTokens;
    private Long maxCompletionTokens;
    private Long maxTotalTokens;
    private Integer maxResources;
    private Integer maxUsers;

    private Long usedPromptTokens;
    private Long usedCompletionTokens;
    private Long usedTotalTokens;
    private Integer usedResources;
    private Integer usedUsers;

    private Boolean threshold80Notified;
    private Boolean threshold100Notified;

    private Long overageTokens;
    private BigDecimal overageCharges;
    private String currency;

    private Instant createdAt;
    private Instant updatedAt;
}
