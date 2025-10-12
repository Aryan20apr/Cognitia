package com.intellidesk.cognitia.chat.service;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.intellidesk.cognitia.chat.models.entities.ChatMessage;
import com.intellidesk.cognitia.chat.models.entities.StoredMessage;
import com.intellidesk.cognitia.chat.repository.ChatMessageRepository;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.beans.factory.annotation.Value;

/**
 * Hydrates Redis-backed ChatMemory with last N messages from DB if needed.
 */
@Service
public class ChatMemoryHydrator {

    private final RedisTemplate<String, StoredMessage> redisTemplate;
    private final ChatMessageRepository messageRepo;
    private final int maxTurns;
    private final String memoryKeyPrefix; // must match ChatMemoryStore prefix

    public ChatMemoryHydrator(RedisTemplate<String, StoredMessage> redisTemplate,
                              ChatMessageRepository messageRepo,
                              @Value("${cognitia.chat.memory.maxTurns:20}") int maxTurns,
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
        String key = memoryKeyPrefix + conversationId;

        Boolean exists = redisTemplate.hasKey(key);
        if (Boolean.TRUE.equals(exists)) {
            // memory exists — optionally check how many turns stored and hydrate if below threshold
            Long size = redisTemplate.opsForList().size(key);
            if (size != null && size >= Math.min(maxTurns, 3)) { // small threshold to avoid rehydration too often
                return;
            }
        }

        // fetch last maxTurns messages (most recent first) and reverse to chronological order
        List<ChatMessage> last = messageRepo.findTopNByThreadIdOrderByCreatedAtDesc(UUID.fromString(conversationId), maxTurns)
                .stream()
                .sorted(Comparator.comparing(ChatMessage::getCreatedAt))
                .collect(Collectors.toList());

        if (last.isEmpty()) return;

        // Build a small in-memory ChatMemory and then push to Redis via RedisTemplate
        // We'll push plain message DTOs that RedisChatMemoryStore understands,
        // OR we can mirror how ChatMemoryStore stores messages (depends on implementation).
        // For portability, store lightweight maps: {role,content,timestamp}
        for (ChatMessage m : last) {
            // var map = new java.util.HashMap<String, Object>();
            // map.put("role", m.getSender().getValue());
            // map.put("content", m.getContent());
            // map.put("timestamp", m.getCreatedAt() != null ? m.getCreatedAt().toString() : Instant.now().toString());
            // redisTemplate.opsForList().rightPush(key, map);

            StoredMessage storedMessage = new StoredMessage();
            storedMessage.setMessageType(m.getSender());
            storedMessage.setText(m.getContent());
            storedMessage.setTimestamp(m.getCreatedAt().toInstant());

            redisTemplate.opsForList().rightPush(key, storedMessage);
        }

        // Optionally set TTL on the memory key
         redisTemplate.expire(key, Duration.ofHours(1));
    }
}
