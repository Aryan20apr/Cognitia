package com.intellidesk.cognitia.analytics.service;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.intellidesk.cognitia.analytics.models.dto.ChatUsageDetailsDTO;

public interface ChatUsageService {

    
    public ChatUsageDetailsDTO saveChatUsage(ChatUsageDetailsDTO chatUsageDetailsDTO);

    public Page<ChatUsageDetailsDTO> getChatUsageData(String userId, String threadId, Pageable pageable);

    public Optional<ChatUsageDetailsDTO> findByRequestId(String requestId);

    
}
