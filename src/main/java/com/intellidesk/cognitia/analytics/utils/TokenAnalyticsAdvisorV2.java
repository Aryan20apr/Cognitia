package com.intellidesk.cognitia.analytics.utils;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

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

        log.info("[TokenAnalyticsAdvisorV2] Starting adviseStream for request with context: {}", request.context());

        return chain.nextStream(request)
            .doOnNext(response -> {
                // Keep track of the last response (which should contain usage metadata)
                lastResponse.set(response);
                try{
                    var usage = response.chatResponse().getMetadata().getUsage();
                    String data = String.format("""
                    [TokenAnalytics]
                    
                    Prompt tokens: %d
                    Completion tokens: %d
                    Total tokens: %d
                   
                    """,
                   
                    usage.getPromptTokens(),
                    usage.getCompletionTokens(),
                    usage.getTotalTokens()
                    );
                    log.info(data);
                } catch (Exception e){
                    log.error("[TokenAnalyticsAdvisorV2] Error extracting usage from response: {}", e.getMessage());
                }
            })
            .doOnComplete(() -> {
                long duration = System.currentTimeMillis() - start;
                ChatClientResponse response = lastResponse.get();
                if (response != null) {
                    log.info("[TokenAnalyticsAdvisorV2] Stream completed, recording usage data.");
                    recordUsageFromResponse(request, response, duration);
                } else {
                    log.warn("[TokenAnalyticsAdvisorV2] Stream completed but no response received for usage tracking");
                }
            })
            .doOnError(e -> {
                log.error("[TokenAnalyticsAdvisorV2] Stream error occurred, usage may not be recorded: {}", e.getMessage());
            });
    }

    private void recordUsageFromResponse(ChatClientRequest request, ChatClientResponse response, long duration) {
        // Extract params
        String tenantIdStr = (String) request.context().get(Constants.PARAM_TENANT_ID);
        String userIdStr = (String) request.context().get(Constants.PARAM_USER_ID);
        String threadIdStr = (String) request.context().get(ChatMemory.CONVERSATION_ID);
        String requestId = (String) request.context().get(Constants.PARAM_REQUEST_ID);

        log.info("[TokenAnalyticsAdvisorV2] Extracted params from request context: tenantId={}, userId={}, threadId={}, requestId={}", tenantIdStr, userIdStr, threadIdStr, requestId);

        UUID tenantId = tenantIdStr != null ? UUID.fromString(tenantIdStr) : null;
        UUID userId = userIdStr != null ? UUID.fromString(userIdStr) : null;
        UUID threadId = threadIdStr != null ? UUID.fromString(threadIdStr) : null;

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

            log.info("[TokenAnalyticsAdvisorV2] Extracted usage from response. Model: {}, PromptTokens: {}, CompletionTokens: {}, TotalTokens: {}, Duration(ms): {}",
                model, promptTokens, completionTokens, totalTokens, duration);

        } else {
            log.info("[TokenAnalyticsAdvisorV2] No usage data found in response for tenantId={}, userId={}, threadId={}", tenantId, userId, threadId);
        }

        // Optionally attach metadata JSON (provider-specific metadata)
        String metadataJson = null;
        try {
            if (response != null && response.chatResponse() != null && response.chatResponse().getMetadata() != null) {
                metadataJson = response.chatResponse().getMetadata().toString();
            
                log.info("[TokenAnalyticsAdvisorV2] Extracted metadata JSON from response: {}", metadataJson);
            }
        } catch (Exception ex) {
            log.warn("[TokenAnalyticsAdvisorV2] Could not extract response metadata JSON: {}", ex.getMessage());
        }

        // Record usage
        if (tenantId != null) {
            log.info("[TokenAnalyticsAdvisorV2] Recording usage: tenantId={}, userId={}, threadId={}, requestId={}", tenantId, userId, threadId, requestId);
            meteringIngestService.recordUsage(tenantId, userId, threadId, requestId,
                    model,
                    promptTokens, completionTokens, totalTokens, metadataJson);
            log.info("[TokenAnalyticsAdvisorV2] Usage recorded for tenantId: {}", tenantId);
        } else {
            log.warn("[TokenAnalyticsAdvisorV2] No tenantId found. Usage will not be recorded.");
        }

        // Always log final token usage for tracking
        log.info("Token usage: tenant={} prompt={} completion={} total={} durationMs={}", 
                tenantId, promptTokens, completionTokens, totalTokens, duration);
    }
}
