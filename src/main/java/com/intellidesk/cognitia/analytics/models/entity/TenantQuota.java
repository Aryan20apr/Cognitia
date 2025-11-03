package com.intellidesk.cognitia.analytics.models.entity;


import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import com.intellidesk.cognitia.analytics.models.enums.QuotaStatus;
import com.intellidesk.cognitia.userandauth.models.entities.TenantAwareEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.Filters;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Tenant quota and usage counters.
 */
@Entity
@Table(name = "tenant_quota")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString
@Filters(@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId"))
public class TenantQuota extends TenantAwareEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "plan_id", referencedColumnName = "id")
    private Plan planId;

    @Column(name = "billing_cycle_start")
    private LocalDate billingCycleStart;

    @Column(name = "billing_cycle_end")
    private LocalDate billingCycleEnd;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private QuotaStatus status = QuotaStatus.ACTIVE;

    @Column(name = "max_prompt_tokens")
    private Long maxPromptTokens;

    @Column(name = "max_completion_tokens")
    private Long maxCompletionTokens;

    @Column(name = "max_total_tokens")
    private Long maxTotalTokens;

    @Column(name = "max_resources")
    private Integer maxResources;

    @OneToMany(mappedBy = "tenantQuota")
    private Set<UserQuota> userQuotas;

    @Column(name = "max_users")
    private Integer maxUsers;

    @Column(name = "used_prompt_tokens")
    private Long usedPromptTokens = 0L;

    @Column(name = "used_completion_tokens")
    private Long usedCompletionTokens = 0L;

    @Column(name = "used_total_tokens")
    private Long usedTotalTokens = 0L;

    @Column(name = "used_resources")
    private Integer usedResources = 0;

    @Column(name = "used_users")
    private Integer usedUsers = 0;

    @Column(name = "threshold_80_notified")
    private Boolean threshold80Notified = false;

    @Column(name = "threshold_100_notified")
    private Boolean threshold100Notified = false;

    @Column(name = "overage_tokens")
    private Long overageTokens = 0L;

    @Column(name = "overage_charges", precision = 19, scale = 4)
    private BigDecimal overageCharges;

    @Column(name = "currency", length = 8)
    private String currency;

    @Column(name = "last_reset_at")
    private Instant lastResetAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

 
}