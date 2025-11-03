package com.intellidesk.cognitia.analytics.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.intellidesk.cognitia.analytics.models.entity.AggregatedUsage;

import java.util.UUID;
import java.time.LocalDate;
import java.util.Optional;

public interface AggregatedUsageRepository extends JpaRepository<AggregatedUsage, UUID> {
    
    @Query("SELECT a FROM AggregatedUsage a WHERE a.tenantId = :tenantId AND a.periodStart = :periodStart")
    Optional<AggregatedUsage> findByTenantIdAndPeriodStart(UUID tenantId, LocalDate periodStart);
}
