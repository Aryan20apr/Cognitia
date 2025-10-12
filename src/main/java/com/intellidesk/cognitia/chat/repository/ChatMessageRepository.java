package com.intellidesk.cognitia.chat.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.intellidesk.cognitia.chat.models.entities.ChatMessage;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    List<ChatMessage> findTop10ByThread_IdOrderByCreatedAtDesc(UUID threadId);

    @Query(value = "SELECT * FROM chat_messages WHERE thread_id = :threadId ORDER BY created_at DESC LIMIT :limit", nativeQuery = true)
    List<ChatMessage> findTopNByThreadIdOrderByCreatedAtDesc(@Param("threadId") UUID threadId, @Param("limit") Integer limit);
}