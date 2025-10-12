package com.intellidesk.cognitia.chat.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.ai.chat.memory.ChatMemory;

import org.springframework.stereotype.Service;

import com.intellidesk.cognitia.chat.models.entities.ChatMessage;
import com.intellidesk.cognitia.chat.repository.ChatMessageRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class SimpleChatMemoryService {

    private final ChatMessageRepository messageRepository;

    public String loadMemoryForThread(UUID threadId) {
        List<ChatMessage> lastMessages = 
            messageRepository.findTop10ByThread_IdOrderByCreatedAtDesc(threadId).reversed();
        
        String history = lastMessages.stream()
            .map(msg -> msg.getSender().name() + ": " + msg.getContent())
            .collect(Collectors.joining("\n"));

        return history;
    }
}

