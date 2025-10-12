package com.intellidesk.cognitia.chat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellidesk.cognitia.chat.models.entities.StoredMessage;

@Configuration
public class RedisMessageConfig {

    @Bean(name = "chatMemoryRedisTemplate")
    public RedisTemplate<String, StoredMessage> redisTemplate(RedisConnectionFactory factory, ObjectMapper objectMapper) {
        RedisTemplate<String, StoredMessage> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // Jackson serializer for StoredMessage
        Jackson2JsonRedisSerializer<StoredMessage> valueSerializer =
                new Jackson2JsonRedisSerializer<>(StoredMessage.class);
        valueSerializer.setObjectMapper(objectMapper);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(valueSerializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(valueSerializer);
        template.afterPropertiesSet();
        return template;
    }
}
