package com.intellidesk.cognitia.chat.repository;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import com.intellidesk.cognitia.chat.models.entities.StoredMessage;

import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.memory.ChatMemoryRepository;

@Repository
@Slf4j
public class RedisChatMemoryRepository implements ChatMemoryRepository {


    private final RedisTemplate<String, StoredMessage> chatMemoryRedisTemplate;

    @Value("${chat.memory.prefix:cognitia:chat:memory:}")
    private String keyPrefix; // e.g. "cognitia:chat:memory:"

    public RedisChatMemoryRepository(@Qualifier("chatMemoryRedisTemplate") RedisTemplate<String, StoredMessage> redisTemplate) {
        this.chatMemoryRedisTemplate = redisTemplate;
    }

    private String key(String conversationId) {
        return keyPrefix + conversationId;
    }

    @Override
    public List<String> findConversationIds() {
        log.info("[RedisChatMemoryRepository] [findConversationIds] Retrieving all conversation IDs from Redis with prefix '{}'", keyPrefix);
        Set<String> keys = chatMemoryRedisTemplate.keys(keyPrefix + "*");
        if (keys == null || keys.isEmpty()) return Collections.emptyList();
        return keys.stream()
                   .map(k -> k.substring(keyPrefix.length()))
                   .collect(Collectors.toList());
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        log.info("[RedisChatMemoryRepository] [findByConversationId] Retrieving messages for conversationId {}", conversationId);
        String k = key(conversationId);
        List<StoredMessage> stored = chatMemoryRedisTemplate.opsForList().range(k, 0, -1);
        if (stored == null) return Collections.emptyList();

        List<Message> messages = new ArrayList<>(stored.size());
        for (StoredMessage s : stored) {
            Message m;
            var mt = s.getMessageType();
            if ("USER".equalsIgnoreCase(mt.getValue())) {
                m = new UserMessage(s.getText());
            } else if ("SYSTEM".equalsIgnoreCase(mt.getValue())) {
                m = new SystemMessage(s.getText());
            } else { // ASSISTANT or others
                // AssistantMessage has constructors in Spring AI; create with text and metadata
                m = new AssistantMessage(s.getText());
            }
            // if metadata present and Message implementations support properties, set them as needed
            messages.add(m);
        }
        return messages;
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
    
      
            log.info("[RedisChatMemoryRepository] [saveAll] Saving {} messages to conversationId {}", 
                         messages == null ? 0 : messages.size(), conversationId);
        
        String k = key(conversationId);
        // Replace: delete old and push new (the interface semantics say "replaces all existing messages")
        chatMemoryRedisTemplate.delete(k);
        if (messages == null || messages.isEmpty()) {
            return;
        }
        List<StoredMessage> toStore = messages.stream().map(m -> {
            String type = m.getMessageType().name();
            String text = m.getText();
            Map<String, Object> metadata = m.getMetadata() != null ? m.getMetadata() : Map.of();
            Instant ts = Instant.now();
            return new StoredMessage(MessageType.valueOf(type), text, metadata, ts);
        }).collect(Collectors.toList());

        chatMemoryRedisTemplate.opsForList().rightPushAll(k, toStore);
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        chatMemoryRedisTemplate.delete(key(conversationId));
    }
}
