package com.intellidesk.cognitia.analytics.utils;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Component;

import com.intellidesk.cognitia.analytics.service.MeteringIngestService;
import com.intellidesk.cognitia.common.Constants;

import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Component
@Slf4j
public class TokenAnalyticsAdvisorV2 implements CallAdvisor {

    private final MeteringIngestService meteringIngestService;

    public TokenAnalyticsAdvisorV2(MeteringIngestService meteringIngestService) {
        this.meteringIngestService = meteringIngestService;
    }

    @Override
    public String getName() {
        return "TokenAnalyticsCallAdvisor";
    }

    @Override
    public int getOrder() {
        return 100; // after enforcement but before logger, choose appropriate order
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        long start = System.currentTimeMillis();

        // proceed with call
        ChatClientResponse response = chain.nextCall(request);

        long duration = System.currentTimeMillis() - start;

        // Extract params
        String tenantIdStr = (String) request.context().get(Constants.PARAM_TENANT_ID);
        String userIdStr = (String) request.context().get(Constants.PARAM_USER_ID);
        String threadIdStr = (String) request.context().get(ChatMemory.CONVERSATION_ID);
        String requestId = (String) request.context().get(Constants.PARAM_REQUEST_ID);

        UUID tenantId = tenantIdStr != null ? UUID.fromString(tenantIdStr) : null;
        UUID userId = userIdStr != null ? UUID.fromString(userIdStr) : null;
        UUID threadId = threadIdStr != null ? UUID.fromString(threadIdStr) : null;

        // Extract usage metadata safely:
        Long promptTokens = null, completionTokens = null, totalTokens = null;
        if (response != null && response.chatResponse().getMetadata() != null && response.chatResponse().getMetadata().getUsage() != null) {
            var usage = response.chatResponse().getMetadata().getUsage();

            String data = String.format("""
                    [TokenAnalytics]
                    Thread: %s
                    Prompt tokens: %d
                    Completion tokens: %d
                    Total tokens: %d
                    Duration: %d ms
                    """,
                    threadId,
                    usage.getPromptTokens(),
                    usage.getCompletionTokens(),
                    usage.getTotalTokens(),
                    duration
            );
            log.info(data);
            log.info("[Token Analytics Call Advisor] response meta data: "+response.chatResponse().getMetadata());

            // usage.promptTokens() etc. (may be Long or Integer depending on provider)
            Number promptN = usage.getPromptTokens();
            Number completionN = usage.getCompletionTokens();
            Number totalN = usage.getTotalTokens();
            promptTokens = promptN != null ? promptN.longValue() : null;
            completionTokens = completionN != null ? completionN.longValue() : null;
            totalTokens = totalN != null ? totalN.longValue() : null;
        }

        // Optionally attach metadata JSON (provider-specific metadata)
        String metadataJson = null;
        try {
            metadataJson = response != null && response.chatResponse().getMetadata() != null ? response.chatResponse().getMetadata().toString() : null;
        } catch (Exception ignored) {}

        // Record usage
        if (tenantId != null) {
            meteringIngestService.recordUsage(tenantId, userId, threadId, requestId,
                    response != null ? response.chatResponse().getMetadata().getModel(): null,
                    promptTokens, completionTokens, totalTokens, metadataJson);
        }

        // Optionally log
        log.info("Token usage: tenant={} prompt={} completion={} total={} durationMs={}", tenantId, promptTokens, completionTokens, totalTokens, duration);

        return response;
    }
}
