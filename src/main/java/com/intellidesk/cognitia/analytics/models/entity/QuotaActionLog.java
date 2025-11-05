package com.intellidesk.cognitia.analytics.models.entity;


import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.Filters;

import com.intellidesk.cognitia.userandauth.models.entities.TenantAwareEntity;

@Entity
@Table(name = "quota_action_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper=true)
@Filters(@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId"))
public class QuotaActionLog extends TenantAwareEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "tenant_quota_id", referencedColumnName = "id")
    private TenantQuota tenantQuota;
    
    private UUID userId;

    @Enumerated(EnumType.STRING)
    private ActionType actionType;

    @Column(columnDefinition = "TEXT")
    private String details; // e.g., "Prompt token limit reached"

    private Instant timestamp;

    public enum ActionType {
        LIMIT_HIT,
        OVERAGE_CHARGED
    }
}

