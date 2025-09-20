package com.intellidesk.cognitia.config.redis;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {
    

    @Bean
    LettuceConnectionFactory jedisConnectionFactory(){
        return new LettuceConnectionFactory();
    }

    @Bean
    public RedisTemplate<String,Object> redisTemplate(){
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        var stringSerializaer = new StringRedisSerializer();
        var jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(Object.class); 
        template.setConnectionFactory(jedisConnectionFactory());
        template.setValueSerializer(jackson2JsonRedisSerializer);
        template.setKeySerializer(stringSerializaer);
        template.setHashKeySerializer(stringSerializaer);
        template.setHashValueSerializer(jackson2JsonRedisSerializer);
        return template;
    }
}
