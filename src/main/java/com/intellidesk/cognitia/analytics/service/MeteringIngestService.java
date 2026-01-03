package com.intellidesk.cognitia.analytics.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.intellidesk.cognitia.analytics.models.dto.ChatUsageDetailsDTO;
import com.intellidesk.cognitia.chat.models.entities.ChatThread;
import com.intellidesk.cognitia.userandauth.models.entities.User;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Slf4j
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
        log.info("Start recording usage: tenantId={}, userId={}, threadId={}, requestId={}, modelName={}", 
            tenantId, userId, threadId, requestId, modelName);

        if (totalTokens == null) {
            totalTokens = (promptTokens == null ? 0L : promptTokens) + (completionTokens == null ? 0L : completionTokens);
        }
        User user = new User();
        user.setId(userId);
        ChatThread thread = new ChatThread();
        thread.setId(threadId);

        ChatUsageDetailsDTO chatUsageDetailsDTO = ChatUsageDetailsDTO.builder()
                                                    .completionTokens(completionTokens)
                                                    .promptTokens(promptTokens)
                                                    .totalTokens(totalTokens)
                                                    .requestId(requestId)
                                                    .modelName(modelName)
                                                    .isProcessed(false)
                                                    .tenantId(tenantId)
                                                    .threadId(threadId)
                                                    .userId(userId)
                                                    .metaDataJson(metadataJson)
                                                    .build();

        log.info("Saving ChatUsageDetailsDTO for tenantId={}, userId={}, threadId={}, totalTokens={}",
                 tenantId, userId, threadId, totalTokens);

        chatUsageService.saveChatUsage(chatUsageDetailsDTO);

        // 2) increment Redis fast counters
        try {
            log.info("Incrementing Redis token counters: tenantId={}, userId={}, totalTokens={}", tenantId, userId, totalTokens);
            redisCounterService.incrementTenantTokens(tenantId, totalTokens);
            if (userId != null) redisCounterService.incrementUserTokens(tenantId, userId, totalTokens);
        } catch (Exception ex) {
            log.warn("Exception incrementing Redis token counters for tenantId={}, userId={} (will be reconciled later): {}", 
                tenantId, userId, ex.getMessage(), ex);
            // Log and proceed — counters will be reconciled later
        }

        // 3) publish event to Kafka for aggregator/billing
        log.info("Publishing usage event to Kafka for tenantId={}, userId={}, threadId={}", tenantId, userId, threadId);
        usageEventProducer.publish(chatUsageDetailsDTO);

        log.info("Finished recording usage: tenantId={}, userId={}, threadId={}, requestId={}", tenantId, userId, threadId, requestId);
    }
}
