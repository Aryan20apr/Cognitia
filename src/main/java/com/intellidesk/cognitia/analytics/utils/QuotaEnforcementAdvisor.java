package com.intellidesk.cognitia.analytics.utils;

import java.util.UUID;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.stereotype.Component;

import com.intellidesk.cognitia.analytics.service.QuotaService;
import com.intellidesk.cognitia.common.Constants;
import com.intellidesk.cognitia.utils.exceptionHandling.QuotaExceededException;

import reactor.core.publisher.Flux;

@Component
public class QuotaEnforcementAdvisor implements CallAdvisor, StreamAdvisor {

    private final QuotaService quotaService;

    public QuotaEnforcementAdvisor(QuotaService quotaService) {
        this.quotaService = quotaService;
    }

    @Override
    public String getName() {
        return "QuotaEnforcementCallAdvisor";
    }

    @Override
    public int getOrder() {
        return -100; // run very early (before token analytics) to pre-check
    }

    /**
     * Common quota check logic - throws QuotaExceededException if quota exceeded
     */
    private void checkQuota(ChatClientRequest request) {
        // Extract parameters (userId, tenantId)
        String tenantIdStr = (String) request.context().get(Constants.PARAM_TENANT_ID);
        String userIdStr = (String) request.context().get(Constants.PARAM_USER_ID);

        UUID tenantId = null;
        UUID userId = null;
        if (tenantIdStr != null) tenantId = UUID.fromString(tenantIdStr);
        if (userIdStr != null) userId = UUID.fromString(userIdStr);

        // Estimate tokens heuristically if caller provided estimate param
        Long estimatedTokens = null;
        Object est = request.context().get("estimatedTokens");
        if (est instanceof Number num) estimatedTokens = num.longValue();

        // If no estimate, compute a conservative estimate from prompt length:
        if (estimatedTokens == null) {
            String prompt = request.prompt().getInstructions() != null && !request.prompt().getInstructions().isEmpty()
                    ? request.prompt().getInstructions().stream().map(m -> m.getText()).reduce("", (a, b) -> a + "\n" + b)
                    : "";
            estimatedTokens = conservativeTokenEstimate(prompt);
        }

        // Pre-check with QuotaService
        boolean allowed = quotaService.canConsume(tenantId, userId, estimatedTokens);

        if (!allowed) {
            // Enforcement logic depends on mode; here, throw exception -> the service layer should map to HTTP 429
            throw new QuotaExceededException("Quota exceeded for tenant: " + tenantId);
        }
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        checkQuota(request);
        // Allow to proceed.
        return chain.nextCall(request);
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        checkQuota(request);
        // Allow to proceed with streaming.
        return chain.nextStream(request);
    }

    private long conservativeTokenEstimate(String prompt) {
        if (prompt == null || prompt.isBlank()) return 10L;
        // naive: assume 1 token per 4 characters
        int chars = prompt.length();
        return Math.max(10L, chars / 4L + 50L); // +50 cushion for completion
    }
}
