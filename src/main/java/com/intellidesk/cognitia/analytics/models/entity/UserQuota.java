package com.intellidesk.cognitia.analytics.models.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.intellidesk.cognitia.userandauth.models.entities.TenantAwareEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Per-user quota and usage counters for a tenant.
 */
@Entity
@Table(name = "user_quota",
       indexes = {
           @Index(name = "idx_user_quota_user_id", columnList = "user_id"),
           @Index(name = "idx_user_quota_tenant_id", columnList = "tenant_id"),
           @Index(name = "idx_user_quota_tenant_quota_id", columnList = "tenant_quota_id")
       })
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString
public class UserQuota extends TenantAwareEntity {

    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne
    @JoinColumn(name = "tenant_quota_id", referencedColumnName = "id")
    private TenantQuota tenantQuota;

    @Column(name = "billing_cycle_start")
    private LocalDate billingCycleStart;

    @Column(name = "billing_cycle_end")
    private LocalDate billingCycleEnd;

    @Column(name = "max_prompt_tokens")
    private Long maxPromptTokens;

    @Column(name = "max_completion_tokens")
    private Long maxCompletionTokens;

    @Column(name = "max_total_tokens")
    private Long maxTotalTokens;

    @Column(name = "max_resources")
    private Integer maxResources;

    @Column(name = "used_prompt_tokens")
    private Long usedPromptTokens = 0L;

    @Column(name = "used_completion_tokens")
    private Long usedCompletionTokens = 0L;

    @Column(name = "used_total_tokens")
    private Long usedTotalTokens = 0L;

    @Column(name = "used_resources")
    private Integer usedResources = 0;

    @Column(name = "threshold_80_notified")
    private Boolean threshold80Notified = false;

    @Column(name = "threshold_100_notified")
    private Boolean threshold100Notified = false;

    @Column(name = "last_activity_at")
    private Instant lastActivityAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;
}