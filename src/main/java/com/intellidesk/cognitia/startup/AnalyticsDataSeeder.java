package com.intellidesk.cognitia.startup;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.intellidesk.cognitia.analytics.models.entity.Plan;
import com.intellidesk.cognitia.analytics.models.entity.TenantQuota;
import com.intellidesk.cognitia.analytics.models.enums.QuotaStatus;
import com.intellidesk.cognitia.analytics.repository.PlanRepository;
import com.intellidesk.cognitia.analytics.repository.TenantQuotaRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Seeds TenantQuota and Plan data for testing.
 * 
 * Runs automatically on startup under the "dev" or "test" profile.
 */
@Component
@Profile({"dev", "test"})  // runs only in these profiles
@Transactional
@Slf4j
public class AnalyticsDataSeeder implements CommandLineRunner {

    private final PlanRepository planRepository;
    private final TenantQuotaRepository tenantQuotaRepository;

    public AnalyticsDataSeeder(PlanRepository planRepository,
                               TenantQuotaRepository tenantQuotaRepository) {
        this.planRepository = planRepository;
        this.tenantQuotaRepository = tenantQuotaRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // 👇 Skip if already seeded
        if (planRepository.count() > 0) {
            System.out.println("✅ Analytics data already seeded. Skipping...");
            return;
        }

        System.out.println("🚀 Seeding test Plans and TenantQuota data...");

        // 1️⃣ Create a sample plan
        Plan standardPlan = Plan.builder()
                .name("Standard Plan")
                .code("TRIAL001")
                .active(true)
                .description("Includes base token and user limits for testing.")
                .includedPromptTokens(50_000L)
                .includedCompletionTokens(50_000L)
                .includedTotalTokens(100_000L)
                .includedDocs(500L)
                .includedUsers(10L)
                .pricePerMonth(new BigDecimal("49.99"))
                // .overagePricing(new BigDecimal("0.0020")) // per token
                .modelRestrictions("gpt-4o-mini,gpt-4o")
                .build();

        planRepository.save(standardPlan);

        // 2️⃣ Create TenantQuota for this plan
        TenantQuota tenantQuota = TenantQuota.builder()
                .planId(standardPlan)
                .status(QuotaStatus.ACTIVE)
                .billingCycleStart(LocalDate.now().withDayOfMonth(1))
                .billingCycleEnd(LocalDate.now().with(TemporalAdjusters.lastDayOfMonth()))
                .maxPromptTokens(50_000L)
                .maxCompletionTokens(50_000L)
                .maxTotalTokens(100_000L)
                .maxResources(500)
                .maxUsers(10)
                .currency("USD")
                .createdAt(java.time.Instant.now())
                .updatedAt(java.time.Instant.now())
                .build();
        tenantQuota.setTenantId(UUID.fromString("0fad3c60-26d6-436e-bbc4-6f3842e757ff"));
        tenantQuotaRepository.save(tenantQuota);

        log.info("✅ Seeded TenantQuota for tenant [%s] under plan [%s]%n",
                tenantQuota.getTenantId(), standardPlan.getName());
    }
}
