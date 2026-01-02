package com.intellidesk.cognitia.analytics.utils;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Component;

import com.intellidesk.cognitia.analytics.service.MeteringIngestService;
import com.intellidesk.cognitia.common.Constants;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Component
@Slf4j
public class TokenAnalyticsAdvisorV2 implements CallAdvisor, StreamAdvisor {

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

        recordUsageFromResponse(request, response, duration);

        return response;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        long start = System.currentTimeMillis();
        AtomicReference<ChatClientResponse> lastResponse = new AtomicReference<>();

        return chain.nextStream(request)
            .doOnNext(response -> {
                // Keep track of the last response (which should contain usage metadata)
                lastResponse.set(response);
            })
            .doOnComplete(() -> {
                long duration = System.currentTimeMillis() - start;
                ChatClientResponse response = lastResponse.get();
                if (response != null) {
                    recordUsageFromResponse(request, response, duration);
                } else {
                    log.warn("Stream completed but no response received for usage tracking");
                }
            })
            .doOnError(e -> {
                log.error("Stream error occurred, usage may not be recorded: {}", e.getMessage());
            });
    }

    private void recordUsageFromResponse(ChatClientRequest request, ChatClientResponse response, long duration) {
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
        String model = null;
        
        if (response != null && response.chatResponse() != null 
                && response.chatResponse().getMetadata() != null 
                && response.chatResponse().getMetadata().getUsage() != null) {
            var usage = response.chatResponse().getMetadata().getUsage();
            model = response.chatResponse().getMetadata().getModel();

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
            log.info("[Token Analytics Call Advisor] response meta data: {}", response.chatResponse().getMetadata());

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
            if (response != null && response.chatResponse() != null && response.chatResponse().getMetadata() != null) {
                metadataJson = response.chatResponse().getMetadata().toString();
            }
        } catch (Exception ignored) {}

        // Record usage
        if (tenantId != null) {
            meteringIngestService.recordUsage(tenantId, userId, threadId, requestId,
                    model,
                    promptTokens, completionTokens, totalTokens, metadataJson);
        }

        // Optionally log
        log.info("Token usage: tenant={} prompt={} completion={} total={} durationMs={}", 
                tenantId, promptTokens, completionTokens, totalTokens, duration);
    }
}
