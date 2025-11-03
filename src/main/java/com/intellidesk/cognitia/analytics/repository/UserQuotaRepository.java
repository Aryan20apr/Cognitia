package com.intellidesk.cognitia.analytics.repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.intellidesk.cognitia.analytics.models.entity.UserQuota;

public interface UserQuotaRepository extends JpaRepository<UserQuota, UUID>{

    @Query("SELECT uq FROM UserQuota uq WHERE uq.userId = :userId AND uq.status = com.intellidesk.cognitia.analytics.models.enums.QuotaStatus.ACTIVE")
    public Optional<UserQuota> findActiveQuotaByUser(UUID userId);

    @Modifying
    @Query("""
        UPDATE UserQuota uq
        SET uq.usedPromptTokens = COALESCE(uq.usedPromptTokens,0) + :promptTokens,
            uq.usedCompletionTokens = COALESCE(uq.usedCompletionTokens,0) + :completionTokens,
            uq.usedTotalTokens = COALESCE(uq.usedTotalTokens,0) + :totalTokens,
            uq.usedResources = COALESCE(uq.usedResources,0) + :resources
        WHERE uq.userId = :userId
          AND :periodStart BETWEEN uq.billingCycleStart AND uq.billingCycleEnd
    """)
    int incrementUsedTokens(UUID userId, LocalDate periodStart,
                            long promptTokens, long completionTokens, long totalTokens, int resources);
}
