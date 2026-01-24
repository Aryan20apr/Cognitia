package com.intellidesk.cognitia.analytics.service.impl;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.intellidesk.cognitia.analytics.models.dto.AssignPlanRequest;
import com.intellidesk.cognitia.analytics.models.dto.ChatUsageDetailsDTO;
import com.intellidesk.cognitia.analytics.models.dto.QuotaProvisionRequest;
import com.intellidesk.cognitia.analytics.models.dto.TenantQuotaDTO;
import com.intellidesk.cognitia.analytics.models.entity.AggregatedUsage;
import com.intellidesk.cognitia.analytics.models.entity.Plan;
import com.intellidesk.cognitia.analytics.models.entity.TenantQuota;
import com.intellidesk.cognitia.analytics.models.entity.UserQuota;
import com.intellidesk.cognitia.analytics.models.enums.EnforcementMode;
import com.intellidesk.cognitia.analytics.models.enums.QuotaStatus;
import com.intellidesk.cognitia.analytics.repository.AggregatedUsageRepository;
import com.intellidesk.cognitia.analytics.repository.ChatUsageRepository;
import com.intellidesk.cognitia.analytics.repository.PlanRepository;
import com.intellidesk.cognitia.analytics.repository.TenantQuotaRepository;
import com.intellidesk.cognitia.analytics.repository.UserQuotaRepository;
import com.intellidesk.cognitia.analytics.service.ChatUsageService;
import com.intellidesk.cognitia.analytics.service.QuotaService;
import com.intellidesk.cognitia.analytics.service.RedisCounterService;
import com.intellidesk.cognitia.analytics.utils.TenantQuotaMapper;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import com.intellidesk.cognitia.analytics.models.enums.PeriodType;

/**
 * Minimal example implementation — adapt to your TenantQuota DB model and Plan
 * pricing
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class QuotaServiceImpl implements QuotaService {

    private final RedisCounterService redisCounterService;
    private final TenantQuotaRepository tenantQuotaRepository;
    private final UserQuotaRepository userQuotaRepository;
    private final ChatUsageService chatUsageService;
    private final ChatUsageRepository chatUsageRepository;
    private final AggregatedUsageRepository aggregatedUsageRepository;
    private final PlanRepository planRepository;
    private final TenantQuotaMapper mapper;

    @Override
    @Transactional
    public boolean canConsume(UUID tenantId, UUID userId, long estimatedTokens) {
        // Fetch tenant quota (fast)
        TenantQuota tQuota = tenantQuotaRepository.findActiveQuotaByTenant(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant quota not found"));
        long tenantLimit = tQuota.getMaxTotalTokens();
        long usedTenant = currentTenantTokens(tenantId);

        // Check tenant allowance
        if (tenantLimit > 0 && (usedTenant + estimatedTokens) > tenantLimit) {
            // tenant exceed
            EnforcementMode mode = getEnforcementMode(tenantId);
            return mode != EnforcementMode.HARD ? true : false;
        }

        // User-level check if applicable
        if (userId != null) {
            UserQuota uQuota = userQuotaRepository.findActiveQuotaByUser(userId)
                    .orElse(null);
            if (uQuota != null && uQuota.getMaxTotalTokens() > 0) {
                long usedUser = currentUserTokens(tenantId, userId);
                if ((usedUser + estimatedTokens) > uQuota.getMaxTotalTokens()) {
                    EnforcementMode mode = getEnforcementMode(tenantId);
                    return mode != EnforcementMode.HARD ? true : false;
                }
            }
        }

        return true;
    }

    @Override
    @Transactional
    public void recordUsage(ChatUsageDetailsDTO chatUsageDetailsDTO) {
        log.info("Recording Chat usage with details: {}", chatUsageDetailsDTO);
        UUID tenantId = chatUsageDetailsDTO.getTenantId();

        String requestId = chatUsageDetailsDTO.getRequestId();
        if (requestId != null) {
            log.info("Checking for existing usage event with requestId={}", requestId);
            Optional<ChatUsageDetailsDTO> existing = chatUsageService.findByRequestId(requestId);
            if (existing.isPresent() && Boolean.TRUE.equals(existing.get().getIsProcessed())) {
                log.info("RequestId {} is already processed for quota updates. Skipping.", requestId);
                return;
            }
        }
        long p = chatUsageDetailsDTO.getPromptTokens() == null ? 0L : chatUsageDetailsDTO.getPromptTokens();
        long c = chatUsageDetailsDTO.getCompletionTokens() == null ? 0L : chatUsageDetailsDTO.getCompletionTokens();
        long total = p + c;
        log.info("Tokens to be recorded: promptTokens={}, completionTokens={}, total={}", p, c, total);

        // Determine billing period start (for example, first day of month)
        LocalDate periodStart = YearMonth.now().atDay(1); // adapt to tenant timezone / billing cycle
        log.info("Using periodStart={} for quota accounting.", periodStart);

        // 2) Atomically increment tenant quota counters in DB (via repository @Modifying)
        int updatedRows = tenantQuotaRepository.incrementUsedTokens(
                tenantId,
                LocalDate.now(),
                p, c, total, 0); // resources = 0 in token-only event
        log.info("Incremented tenant quota counters: updatedRows={}", updatedRows);

        // 3) Atomically increment user quota counters if user quota exists
        if (chatUsageDetailsDTO.getUserId() != null) {
            UUID userId = chatUsageDetailsDTO.getUserId();
            int userUpdated = userQuotaRepository.incrementUsedTokens(userId, periodStart, p, c, total, 0);
            log.info("Incremented user quota counters for userId {}: userUpdated={}", userId, userUpdated);
            // if userUpdated == 0, user quota not configured; it's OK
        }

        // 4) Mark ChatUsageEvent as processed (if exists) to prevent double processing
        if (requestId != null) {
            chatUsageRepository.findByRequestId(requestId).ifPresent(ev -> {
                log.info("Marking ChatUsageEvent with requestId {} as processed.", requestId);
                ev.setIsProcessed(true);
                ev.setProcessedAt(Date.from(Instant.now()));
                chatUsageRepository.save(ev);
            });
        }

        // 5) Update aggregated usage (durable snapshot) for the tenant — optional
        aggregatedUsageRepository.findByTenantIdAndPeriodStart(tenantId, periodStart)
                .ifPresentOrElse(agg -> {
                    log.info("Updating aggregated usage for tenantId={}, periodStart={}", tenantId, periodStart);
                    agg.setTotalPromptTokens(
                            (agg.getTotalPromptTokens() == null ? 0L : agg.getTotalPromptTokens()) + p);
                    agg.setTotalCompletionTokens(
                            (agg.getTotalCompletionTokens() == null ? 0L : agg.getTotalCompletionTokens()) + c);
                    agg.setTotalTokens((agg.getTotalTokens() == null ? 0L : agg.getTotalTokens()) + total);
                    aggregatedUsageRepository.save(agg);
                }, () -> {
                    log.info("No aggregated usage record found for tenantId={}, periodStart={}. Creating new.", tenantId, periodStart);
                    AggregatedUsage newAgg = new AggregatedUsage();
                    newAgg.setTenantId(tenantId);
                    newAgg.setPeriodStart(periodStart);
                    newAgg.setPeriod(PeriodType.MONTH);
                    newAgg.setTotalPromptTokens(p);
                    newAgg.setTotalCompletionTokens(c);
                    newAgg.setTotalTokens(total);
                    aggregatedUsageRepository.save(newAgg);
                });

        // 6) Compute overage and update TenantQuota.overage/overageCharges if needed
        TenantQuota t = tenantQuotaRepository.findActiveQuotaByTenant(tenantId)
                .orElse(null);

        if (t != null && t.getMaxTotalTokens() != null && t.getMaxTotalTokens() > 0) {
            long used = t.getUsedTotalTokens() == null ? total : t.getUsedTotalTokens();
            long overage = Math.max(0L, used - t.getMaxTotalTokens());
            t.setOverageTokens(overage);
            log.info("Computed overage for tenantId={}: used={}, maxTotal={}, overage={}", tenantId, used, t.getMaxTotalTokens(), overage);

            // cost calculation: simple example: overage * ratePerToken
            BigDecimal rate = t.getOverageCharges() != null ? t.getOverageCharges() : BigDecimal.ZERO;
            BigDecimal overageChargeAmount = rate.multiply(BigDecimal.valueOf(overage));
            t.setOverageCharges(overageChargeAmount);
            log.info("Updated overageCharges for tenantId={}: rate={}, overageChargeAmount={}", tenantId, rate, overageChargeAmount);

            tenantQuotaRepository.save(t);
        }
        log.info("Successfully recorded usage for tenantId={}, requestId={}", tenantId, requestId);
    }

    @Override
    @Transactional
    public EnforcementMode getEnforcementMode(UUID tenantId) {
        // Load tenant plan settings; default to HYBRID
        TenantQuota t = tenantQuotaRepository.findActiveQuotaByTenant(tenantId)
                .orElse(null);
        if (t == null)
            return EnforcementMode.HYBRID;
        // Map your DB field to mode. For example, a column enforcement_mode
        // For prototype, return HYBRID
        return EnforcementMode.HYBRID;
    }

    @Override
    @Transactional
    public long getTenantRemainingTokens(UUID tenantId) {
        TenantQuota tQuota = tenantQuotaRepository.findActiveQuotaByTenant(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant quota not found"));
        long limit = tQuota.getMaxTotalTokens();
        long used = currentTenantTokens(tenantId);
        return Math.max(0, limit - used);
    }

    private long currentTenantTokens(UUID tenantId) {
        // Read from Redis; fallback to DB aggregated snapshots if missing
        // For brevity, call redisCounterService and parse String.
        try {
            String key = redisCounterService.tenantTokensKey(tenantId);
            String val = redisCounterService.getValue(key);
            return val == null ? 0L : Long.parseLong(val);
        } catch (Exception ex) {
            // fallback: scan aggregated usage table
            return tenantQuotaRepository.getUsedTotalTokens(tenantId).orElse(0L);
        }
    }

    private long currentUserTokens(UUID tenantId, UUID userId) {
        try {
            String key = redisCounterService.userTokensKey(tenantId, userId);
            String val = redisCounterService.getValue(key);
            return val == null ? 0L : Long.parseLong(val);
        } catch (Exception ex) {
            return 0L;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public TenantQuotaDTO getTenantQuota(UUID tenantId) {
        TenantQuota quota = tenantQuotaRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant quota not found"));
        return mapper.toDto(quota);
    }

    @Override
    @Transactional
    public TenantQuotaDTO assignPlan(UUID tenantId, AssignPlanRequest request) {
        Plan plan = planRepository.findById(request.getPlanId())
                .orElseThrow(() -> new IllegalArgumentException("Plan not found"));

        TenantQuota quota = tenantQuotaRepository.findByTenantId(tenantId)
                .orElseGet(() -> TenantQuota.builder()

                        .status(QuotaStatus.ACTIVE)
                        .build());
        quota.setTenantId(tenantId);
        quota.setPlanId(plan);
        quota.setBillingCycleStart(LocalDate.now());
        quota.setBillingCycleEnd(LocalDate.now().plusMonths(1).minusDays(1));

        quota.setMaxPromptTokens(plan.getIncludedPromptTokens());
        quota.setMaxCompletionTokens(plan.getIncludedCompletionTokens());
        quota.setMaxTotalTokens(plan.getIncludedTotalTokens());
        quota.setMaxResources(plan.getIncludedDocs() != null ? plan.getIncludedDocs().intValue() : null);
        quota.setMaxUsers(plan.getIncludedUsers() != null ? plan.getIncludedUsers().intValue() : null);

        if (request.isResetUsage()) {
            quota.setUsedPromptTokens(0L);
            quota.setUsedCompletionTokens(0L);
            quota.setUsedTotalTokens(0L);
            quota.setUsedResources(0);
            quota.setUsedUsers(0);
        }

        TenantQuota saved = tenantQuotaRepository.save(quota);
        return mapper.toDto(saved);
    }

    @Override
    public TenantQuotaDTO provisionQuota(UUID tenantId, QuotaProvisionRequest request) {
        TenantQuota quota = tenantQuotaRepository.findByTenantId(tenantId)
                .orElseGet(() -> TenantQuota.builder()

                        .status(QuotaStatus.ACTIVE)
                        .build());
        quota.setTenantId(tenantId);
        quota.setMaxPromptTokens(request.getMaxPromptTokens());
        quota.setMaxCompletionTokens(request.getMaxCompletionTokens());
        quota.setMaxTotalTokens(request.getMaxTotalTokens());
        quota.setMaxResources(request.getMaxResources());
        quota.setMaxUsers(request.getMaxUsers());
        if (request.getEnforcementMode() != null)
            quota.setEnforcementMode(request.getEnforcementMode());

        TenantQuota saved = tenantQuotaRepository.save(quota);
        return mapper.toDto(saved);
    }

    @Override
    @Transactional
    public void renewQuotaCycle(UUID tenantId) {
        TenantQuota quota = tenantQuotaRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant quota not found"));

        LocalDate today = LocalDate.now();

        // 1️⃣ Check if billing cycle has ended
        if (quota.getBillingCycleEnd() != null && !today.isAfter(quota.getBillingCycleEnd())) {
            return; // Still within active period — skip
        }

        // 2️⃣ Reset usage counters
        quota.setUsedPromptTokens(0L);
        quota.setUsedCompletionTokens(0L);
        quota.setUsedTotalTokens(0L);
        quota.setUsedResources(0);
        quota.setUsedUsers(0);
        quota.setThreshold80Notified(false);
        quota.setThreshold100Notified(false);
        quota.setOverageTokens(0L);
        quota.setOverageCharges(null);

        // 3️⃣ Advance billing cycle window
        LocalDate newStart = quota.getBillingCycleEnd() != null
                ? quota.getBillingCycleEnd().plusDays(1)
                : today;
        LocalDate newEnd = newStart.plusMonths(1).minusDays(1);
        quota.setBillingCycleStart(newStart);
        quota.setBillingCycleEnd(newEnd);

        // 4️⃣ Optionally update plan quotas (if plan changed or dynamic)
        if (quota.getPlanId() != null) {
            Plan plan = quota.getPlanId();
            quota.setMaxPromptTokens(plan.getIncludedPromptTokens());
            quota.setMaxCompletionTokens(plan.getIncludedCompletionTokens());
            quota.setMaxTotalTokens(plan.getIncludedTotalTokens());
            quota.setMaxResources(plan.getIncludedDocs() != null ? plan.getIncludedDocs().intValue() : null);
            quota.setMaxUsers(plan.getIncludedUsers() != null ? plan.getIncludedUsers().intValue() : null);
        }

        // 5️⃣ Update reset time
        quota.setLastResetAt(java.time.Instant.now());

        tenantQuotaRepository.save(quota);

        // 6️⃣ Emit a billing event (optional)
        // billingService.generateInvoiceForTenant(tenantId);
    }

}