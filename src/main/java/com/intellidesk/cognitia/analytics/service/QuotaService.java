package com.intellidesk.cognitia.analytics.service;


import java.util.UUID;

import com.intellidesk.cognitia.analytics.models.dto.ChatUsageDetailsDTO;
import com.intellidesk.cognitia.analytics.models.enums.EnforcementMode;

public interface QuotaService {


    /** Check quickly (fast) whether request of `estimatedTokens` can proceed. */
    boolean canConsume(UUID tenantId, UUID userId, long estimatedTokens);

    /** Decrement / record usage (used by MeteringIngestService or advisors if needed) — recommended to use MeteringIngestService instead */
    void recordUsage(ChatUsageDetailsDTO chatUsageDetailsDTO);

    /** Get the enforcement mode for this tenant */
    EnforcementMode getEnforcementMode(UUID tenantId);

    /** Get remaining allowance (approx) for tenant */
    long getTenantRemainingTokens(UUID tenantId);

    // admin methods: provision quotas, assign plan, etc.
    void provisionTenantQuota(UUID tenantId /*... spec ...*/);
}

