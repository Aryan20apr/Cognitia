package com.intellidesk.cognitia.chat.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.intellidesk.cognitia.chat.models.entities.ChatMessage;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    List<ChatMessage> findTop10ByThread_IdOrderByCreatedAtDesc(UUID threadId);

    
}