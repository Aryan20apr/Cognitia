package com.intellidesk.cognitia.analytics.service;



import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RedisIdempotencyService {

    private final StringRedisTemplate redisTemplate;
    private static final String KEY_FMT = "usage:request:%s"; // requestId

    // Defaults 
    private static final Duration PROCESSING_TTL = Duration.ofMinutes(8);
    private static final Duration PROCESSED_TTL = Duration.ofDays(1);

    public RedisIdempotencyService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private String key(String requestId) {
        return String.format(KEY_FMT, requestId);
    }

    /**
     * Try to acquire processing lock for requestId.
     * Returns true if lock acquired (no one else processing).
     * If false, someone else might be processing or it's already processed.
     */
    public boolean tryAcquire(String requestId) {
        String k = key(requestId);
        // setIfAbsent with TTL
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(k, "processing", PROCESSING_TTL);
        return acquired != null && acquired;
    }

    /**
     * If the request was already processed, mark it in redis so future calls see it's processed.
     */
    public void markProcessed(String requestId) {
        String k = key(requestId);
        try {
            redisTemplate.opsForValue().set(k, "processed", PROCESSED_TTL);
        } catch (Exception e) {
            // best-effort; processing is guaranteed by DB, redis mark is for early response optimization
        }
    }

    /**
     * Check redis state quickly:
     * - null => no info
     * - "processing" => someone is processing
     * - "processed" => completed
     */
    public String peek(String requestId) {
        if (requestId == null) return null;
        String k = key(requestId);
        try {
            return redisTemplate.opsForValue().get(k);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Force release (rarely used): delete key
     */
    public void release(String requestId) {
        try {
            redisTemplate.delete(key(requestId));
        } catch (Exception ignored) {}
    }
}

