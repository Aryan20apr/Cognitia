package com.intellidesk.cognitia.analytics.service;

import java.time.Duration;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RedisIdempotencyService {

    private final StringRedisTemplate redisTemplate;
    private static final String KEY_FMT = "usage:request:%s"; // requestId

    // Defaults 
    private static final Duration PROCESSING_TTL = Duration.ofMinutes(8);
    private static final Duration PROCESSED_TTL = Duration.ofDays(1);

    /**
     * Lua script for safe markProcessed: only transitions "processing" → "processed".
     * Prevents a stale Kafka event from overwriting a retried request's state.
     * Returns 1 if updated, 0 if skipped (key was not in "processing" state).
     */
    private static final RedisScript<Long> MARK_PROCESSED_SCRIPT = RedisScript.of(
        "local current = redis.call('get', KEYS[1]) " +
        "if current == 'processing' then " +
        "  redis.call('set', KEYS[1], ARGV[1]) " +
        "  redis.call('expire', KEYS[1], tonumber(ARGV[2])) " +
        "  return 1 " +
        "else " +
        "  return 0 " +
        "end",
        Long.class
    );

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
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(k, "processing", PROCESSING_TTL);
        return acquired != null && acquired;
    }

    /**
     * Mark request as processed. Uses a Lua script to atomically transition
     * only from "processing" → "processed", preventing stale events from
     * overwriting a retried request's state.
     * Best-effort: DB-level idempotency is the guarantee.
     */
    public void markProcessed(String requestId) {
        String k = key(requestId);
        try {
            Long result = redisTemplate.execute(
                MARK_PROCESSED_SCRIPT,
                List.of(k),
                "processed",
                String.valueOf(PROCESSED_TTL.getSeconds())
            );
            if (result != null && result == 1) {
                log.debug("[Idempotency] Marked requestId [{}] as processed", requestId);
            } else {
                log.debug("[Idempotency] requestId [{}] not in 'processing' state, skipping mark", requestId);
            }
        } catch (Exception e) {
            log.warn("[Idempotency] Failed to mark requestId [{}] as processed: {}", requestId, e.getMessage());
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
            log.warn("[Idempotency] Failed to peek requestId [{}]: {}", requestId, e.getMessage());
            return null;
        }
    }

    /**
     * Release the processing lock so the request can be retried.
     * Used on errors/cancellations to avoid blocking retries for the TTL duration.
     */
    public void release(String requestId) {
        try {
            redisTemplate.delete(key(requestId));
        } catch (Exception e) {
            log.warn("[Idempotency] Failed to release requestId [{}]: {}. Key will expire after TTL.", 
                requestId, e.getMessage());
        }
    }
}

