package com.intellidesk.cognitia.analytics.utils;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;


import org.springframework.stereotype.Component;

import com.intellidesk.cognitia.analytics.service.QuotaService;
import com.intellidesk.cognitia.common.Constants;
import com.intellidesk.cognitia.utils.exceptionHandling.QuotaExceededException;

import java.util.UUID;

@Component
public class QuotaEnforcementAdvisor implements CallAdvisor {

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

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {

        // Extract parameters (userId, tenantId, requestId)
        String tenantIdStr = (String) request.context().get(Constants.PARAM_TENANT_ID);
        String userIdStr = (String) request.context().get(Constants.PARAM_USER_ID);
        // String requestId = (String) request.context().get(QuotaConstants.PARAM_REQUEST_ID);

        UUID tenantId = null;
        UUID userId = null;
        if (tenantIdStr != null) tenantId = UUID.fromString(tenantIdStr);
        if (userIdStr != null) userId = UUID.fromString(userIdStr);

        // Estimate tokens heuristically if caller provided estimate param
        Long estimatedTokens = null;
        Object est = request.context().get("estimatedTokens");
        if (est instanceof Number) estimatedTokens = ((Number) est).longValue();

        // If no estimate, compute a conservative estimate from prompt length:
        if (estimatedTokens == null) {
            String prompt = request.prompt().getInstructions() != null && !request.prompt().getInstructions().isEmpty()
                    ? request.prompt().getInstructions().stream().map(m -> m.getText()).reduce("", (a,b) -> a + "\n" + b)
                    : "";
            estimatedTokens = conservativeTokenEstimate(prompt);
        }

        // Pre-check with QuotaService
        boolean allowed = quotaService.canConsume(tenantId, userId, estimatedTokens);

        if (!allowed) {
            // Enforcement logic depends on mode; here, throw exception -> the service layer should map to HTTP 429
            throw new QuotaExceededException("Quota exceeded for tenant: " + tenantId);
        }

        // Allow to proceed.
        return chain.nextCall(request);
    }

    private long conservativeTokenEstimate(String prompt) {
        if (prompt == null || prompt.isBlank()) return 10L;
        // naive: assume 1 token per 4 characters
        int chars = prompt.length();
        return Math.max(10L, chars / 4L + 50L); // +50 cushion for completion
    }
}
