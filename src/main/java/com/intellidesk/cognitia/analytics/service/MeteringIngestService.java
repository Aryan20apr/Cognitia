package com.intellidesk.cognitia.analytics.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.intellidesk.cognitia.analytics.models.dto.ChatUsageDetailsDTO;
import com.intellidesk.cognitia.chat.models.entities.ChatThread;
import com.intellidesk.cognitia.userandauth.models.entities.User;
import com.intellidesk.cognitia.userandauth.multiteancy.TenantContext;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MeteringIngestService {

    private final RedisCounterService redisCounterService;
    private final ChatUsageService chatUsageService;
    private final UsageEventProducer usageEventProducer;
    /**
     * Record usage from a successful call.
     * Idempotent: uses requestIdGuard to avoid double counting.
     */
    @Transactional
    public void recordUsage(UUID tenantId, UUID userId, UUID threadId, String requestId,
                            String modelName, Long promptTokens, Long completionTokens, Long totalTokens, String metadataJson) {
                                
        if(totalTokens == null){
             totalTokens =   (promptTokens == null ? 0L : promptTokens) + (completionTokens == null ? 0L : completionTokens); }
        User user = new User();
        user.setId(userId);
        ChatThread thread = new ChatThread();
        thread.setId(threadId);
        // 1) persist audit record (optional: you can sample to reduce DB)
        // ChatUsage event = new ChatUsage();
        // event.setTenantId(tenantId);
        // event.setUser(user);
        // event.setThread(thread);
        // event.setRequestId(requestId);
        // event.setModelName(modelName);
        // event.setPromptTokens(promptTokens);
        // event.setCompletionTokens(completionTokens);
        // event.setTotalTokens(totalTokens);
        // event.setMetaDataJson(metadataJson);

         ChatUsageDetailsDTO chatUsageDetailsDTO = ChatUsageDetailsDTO.builder()
                                                    .completionTokens(completionTokens)
                                                    .promptTokens(promptTokens)
                                                    .totalTokens(totalTokens)
                                                    .requestId(requestId)
                                                    .modelName(modelName)
                                                    .tenantId(TenantContext.getTenantId())
                                                    .threadId(threadId)
                                                    .userId(userId)
                                                    .metaDataJson(metadataJson)
                                                    .build();
        

        chatUsageService.saveChatUsage(chatUsageDetailsDTO);

        // 2) increment Redis fast counters
        try {
            redisCounterService.incrementTenantTokens(tenantId, totalTokens);
            if (userId != null) redisCounterService.incrementUserTokens(tenantId, userId, totalTokens);
        } catch (Exception ex) {
            // Log and proceed — counters will be reconciled later
        }

        // 3) publish event to Kafka for aggregator/billing
       

        usageEventProducer.publish(chatUsageDetailsDTO);
    }
}

