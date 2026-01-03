package com.intellidesk.cognitia.analytics.models.entity;


import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.UUID;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.Filters;

import com.intellidesk.cognitia.userandauth.models.entities.TenantAwareEntity;
import com.intellidesk.cognitia.analytics.models.enums.PeriodType;
@Entity
@Table(name = "aggregated_usage", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"tenant_id", "period_start"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@Filters(@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId"))
public class AggregatedUsage extends TenantAwareEntity {

    @Id
    @GeneratedValue
    private UUID id;

    private UUID userId; // nullable for tenant-level aggregation

    @Enumerated(EnumType.STRING)
    private PeriodType period; // DAY, MONTH

    private LocalDate periodStart; // represents start of the period

    private Long totalPromptTokens;
    private Long totalCompletionTokens;
    private Long totalTokens;

    private Double estimatedCost;


}

