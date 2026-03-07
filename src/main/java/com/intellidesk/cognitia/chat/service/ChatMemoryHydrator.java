package com.intellidesk.cognitia.chat.service;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.intellidesk.cognitia.chat.models.entities.ChatMessage;
import com.intellidesk.cognitia.chat.models.entities.StoredMessage;
import com.intellidesk.cognitia.chat.repository.ChatMessageRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Hydrates Redis-backed ChatMemory with last N messages from DB if needed.
 */
@Service
@Slf4j
public class ChatMemoryHydrator {

    private final RedisTemplate<String, StoredMessage> redisTemplate;
    private final ChatMessageRepository messageRepo;
    private final int maxTurns;
    private final String memoryKeyPrefix; // must match ChatMemoryStore prefix

    public ChatMemoryHydrator(RedisTemplate<String, StoredMessage> redisTemplate,
                              ChatMessageRepository messageRepo,
                              @Value("${cognitia.chat.memory.maxTurns:50}") int maxTurns,
                              @Value("${cognitia.chat.memory.prefix:cognitia:chat:memory:}") String memoryKeyPrefix) {
        this.redisTemplate = redisTemplate;
        this.messageRepo = messageRepo;
        this.maxTurns = maxTurns;
        this.memoryKeyPrefix = memoryKeyPrefix;
    }

    /**
     * Ensure the memory key for conversationId has recent context. If not, hydrate it from DB.
     */
    @Transactional(readOnly = true)
    public void hydrateIfEmpty(String conversationId) {
        try {
            String key = memoryKeyPrefix + conversationId;

        Boolean exists = redisTemplate.hasKey(key);
        if (Boolean.TRUE.equals(exists)) {
            Long size = redisTemplate.opsForList().size(key);
            if (size != null && size >= Math.min(maxTurns, 3)) {
                return;
            }
        }

        List<ChatMessage> last = messageRepo.findTopNByThreadIdOrderByCreatedAtDesc(UUID.fromString(conversationId), maxTurns)
                .stream()
                .sorted(Comparator.comparing(ChatMessage::getCreatedAt))
                .collect(Collectors.toList());

        if (last.isEmpty()) return;

        for (ChatMessage m : last) {
            StoredMessage storedMessage = new StoredMessage();
            storedMessage.setMessageType(m.getSender());
            storedMessage.setText(m.getContent());
            storedMessage.setTimestamp(m.getCreatedAt().toInstant());

            redisTemplate.opsForList().rightPush(key, storedMessage);
        }

            redisTemplate.expire(key, Duration.ofHours(1));
        } catch (Exception e) {
            log.error("[ChatMemoryHydrator] Failed to hydrate memory for {}: {}", conversationId, e.getMessage(), e);
        }
    }
}
