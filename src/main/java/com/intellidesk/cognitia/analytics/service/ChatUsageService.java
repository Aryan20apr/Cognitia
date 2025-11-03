package com.intellidesk.cognitia.analytics.service;

import java.util.List;
import java.util.Optional;

import com.intellidesk.cognitia.analytics.models.dto.ChatUsageDetailsDTO;

public interface ChatUsageService {

    
    public ChatUsageDetailsDTO saveChatUsage(ChatUsageDetailsDTO chatUsageDetailsDTO);

    public List<ChatUsageDetailsDTO> getChatUsageData(String userId, String teantId, String threadId);

    public Optional<ChatUsageDetailsDTO> findByRequestId(String requestId);

    
}
