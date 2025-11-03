package com.intellidesk.cognitia.analytics.repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.intellidesk.cognitia.analytics.models.entity.TenantQuota;

public interface TenantQuotaRepository extends JpaRepository<TenantQuota, UUID> {

    @Query("SELECT tq FROM TenantQuota tq WHERE tq.tenantId = :tenantId AND tq.status = com.intellidesk.cognitia.analytics.models.enums.QuotaStatus.ACTIVE")
    public Optional<TenantQuota> findActiveQuotaByTenant(UUID tenantId);

    @Query("SELECT tq.usedTotalTokens FROM TenantQuota tq WHERE tq.tenantId = :tenantId AND tq.status = com.intellidesk.cognitia.analytics.models.enums.QuotaStatus.ACTIVE")
    public Optional<Long> getUsedTotalTokens(UUID tenantId);

    @Query("SELECT t.usedTotalTokens FROM TenantQuota t WHERE t.tenantId = :tenantId AND :periodStart BETWEEN t.billingCycleStart AND t.billingCycleEnd")
    Optional<Long> findUsedTotalTokens(UUID tenantId, LocalDate periodStart);


    @Modifying
    @Query("""
        UPDATE TenantQuota tq
        SET tq.usedPromptTokens = COALESCE(tq.usedPromptTokens,0) + :promptTokens,
            tq.usedCompletionTokens = COALESCE(tq.usedCompletionTokens,0) + :completionTokens,
            tq.usedTotalTokens = COALESCE(tq.usedTotalTokens,0) + :totalTokens,
            tq.usedResources = COALESCE(tq.usedResources,0) + :resources
        WHERE tq.tenantId = :tenantId
          AND :periodStart BETWEEN tq.billingCycleStart AND tq.billingCycleEnd
    """)
    Integer incrementUsedTokens(UUID tenantId, LocalDate periodStart, long promptTokens, long completionTokens, long totalTokens, int resources);
}
