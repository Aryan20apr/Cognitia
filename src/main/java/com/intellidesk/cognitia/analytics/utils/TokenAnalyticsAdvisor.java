package com.intellidesk.cognitia.analytics.utils;


import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;

import com.intellidesk.cognitia.analytics.models.dto.ChatUsageDetailsDTO;
import com.intellidesk.cognitia.analytics.service.ChatUsageService;
import com.intellidesk.cognitia.userandauth.multiteancy.TenantContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class TokenAnalyticsAdvisor implements CallAdvisor {

    private final ChatUsageService chatUsageService;

    @Override
    public String getName() {
        return "TokenAnalyticsCallAdvisor";
    }

    @Override
    public int getOrder() {
        return 0; // Execute first in the chain
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {

        UUID userId = null;
        if (chatClientRequest.context().containsKey("userId")) {
            String userIdStr = (String) chatClientRequest.context().get("userId");
            if (userIdStr != null && !userIdStr.trim().isEmpty()) {
                try {
                    userId = UUID.fromString(userIdStr);
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid userId format: {}", userIdStr);
                }
            }
        }
        // Extract thread ID from request parameters
        UUID threadId = null;
        if (chatClientRequest.context().containsKey(ChatMemory.CONVERSATION_ID)) {
            String threadIdStr = (String) chatClientRequest.context().get(ChatMemory.CONVERSATION_ID);
            if (threadIdStr != null && !threadIdStr.trim().isEmpty()) {
                try {
                    threadId = UUID.fromString(threadIdStr);
                } catch (IllegalArgumentException e) {
                    log.error("Invalid threadId format: {}", threadIdStr);
                    return callAdvisorChain.nextCall(chatClientRequest); // Skip analytics if threadId is invalid
                }
            }
        }
        // Start measuring time
        long startTime = System.currentTimeMillis();
        // Proceed to the next advisor in the chain
        ChatClientResponse response = callAdvisorChain.nextCall(chatClientRequest);
        // Calculate the duration
        long duration = System.currentTimeMillis() - startTime;
        // Log token usage details
        if (response.chatResponse().getMetadata() != null && response.chatResponse().getMetadata().getUsage() != null && threadId != null) {
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

            saveConsumptionData(response.chatResponse(), threadId, userId);
        } else if (threadId == null) {
            log.warn("[Token Analytics Call Advisor] Skipping analytics - threadId is null");
        }

        return response;
    }

    public void saveConsumptionData(ChatResponse chatResponse, UUID threadUuid, UUID userUuid){
        var usage = chatResponse.getMetadata().getUsage();
        ChatUsageDetailsDTO chatUsageDetailsDTO = ChatUsageDetailsDTO.builder()
                                                    .completionTokens(usage.getCompletionTokens().longValue())
                                                    .promptTokens(usage.getPromptTokens().longValue())
                                                    .totalTokens(usage.getTotalTokens().longValue())
                                                    .tenantId(TenantContext.getTenantId())
                                                    .threadId(threadUuid)
                                                    .userId(userUuid)
                                                    .build();
        ChatUsageDetailsDTO result =  chatUsageService.saveChatUsage(chatUsageDetailsDTO);
    log.info("[Token Analytics Call Advisor] Saved chat usage details: " + result);
    }
}