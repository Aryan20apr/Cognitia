package com.intellidesk.cognitia.chat.service;

import java.time.Duration;
import java.util.UUID;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * Service for managing distributed locks on chat threads.
 * Prevents concurrent message processing on the same thread to ensure
 * message ordering and correct conversation context.
 */
@Service
@Slf4j
public class ThreadLockService {

    private final StringRedisTemplate redisTemplate;
    
    private static final String LOCK_KEY_FMT = "thread:lock:%s";
    private static final String QUEUE_KEY_FMT = "thread:queue:%s";
    private static final Duration LOCK_TTL = Duration.ofMinutes(5); // Max streaming duration
    private static final RedisScript<Long> RELEASE_SCRIPT = RedisScript.of(
        """
        if redis.call('get', KEYS[1]) == ARGV[1] then
            return redis.call('del', KEYS[1])
        else
            return 0
        end
        """
        , Long.class);

    public ThreadLockService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Try to acquire processing lock for a thread.
     * @param threadId The thread ID to lock
     * @return Lock token if acquired, null if thread is busy
     */
    public String tryAcquire(UUID threadId) {
        String key = lockKey(threadId);
        String lockToken = UUID.randomUUID().toString();
        
        try {
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, lockToken, LOCK_TTL);
            if (acquired != null && acquired) {
                log.info("[ThreadLock] Lock acquired for thread {} with token {}", threadId, lockToken);
                return lockToken;
            }
            log.info("[ThreadLock] Failed to acquire lock for thread {} - already locked", threadId);
            return null;
        } catch (Exception e) {
            log.error("[ThreadLock] Error acquiring lock for thread {}: {}", threadId, e.getMessage());
            return null;
        }
    }

    /**
     * Release the lock (only if we own it)
     * @param threadId The thread ID
     * @param lockToken The token received when lock was acquired
     */
    public void release(UUID threadId, String lockToken) {
        if (lockToken == null) return;
        
        String key = lockKey(threadId);
        try {
            Long result = redisTemplate.execute(RELEASE_SCRIPT,List.of(key),lockToken);
            if (result != null && result == 1) {
                log.info("[ThreadLock] Lock released for thread {}", threadId);
            } else {
                log.warn("[ThreadLock] Cannot release lock for thread {} - token mismatch or expired", threadId);
            }
        } catch (Exception e) {
            log.error("[ThreadLock] Error releasing lock for thread {}: {}", threadId, e.getMessage());
        }
    }

    /**
     * Check if thread is currently locked
     * @param threadId The thread ID
     * @return true if thread is locked
     */
    public boolean isLocked(UUID threadId) {
        String key = lockKey(threadId);
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("[ThreadLock] Error checking lock for thread {}: {}", threadId, e.getMessage());
            return false;
        }
    }

    /**
     * Get the current queue position for a thread.
     * This is the number of pending messages waiting to be processed.
     * @param threadId The thread ID
     * @return Queue size (0 if empty or not locked)
     */
    public long getQueuePosition(UUID threadId) {
        String key = queueKey(threadId);
        try {
            Long size = redisTemplate.opsForList().size(key);
            return size != null ? size : 0;
        } catch (Exception e) {
            log.error("[ThreadLock] Error getting queue position for thread {}: {}", threadId, e.getMessage());
            return 0;
        }
    }

    /**
     * Add a message to the thread's processing queue.
     * Used for FIFO queue implementation (Tier 3).
     * @param threadId The thread ID
     * @param messageId The message ID to queue
     * @return The position in queue (1-based)
     */
    public long enqueue(UUID threadId, String messageId) {
        String key = queueKey(threadId);
        try {
            Long position = redisTemplate.opsForList().rightPush(key, messageId);
            // Set expiry on queue to prevent orphaned queues
            redisTemplate.expire(key, Duration.ofHours(1));
            log.info("[ThreadLock] Message {} queued for thread {} at position {}", messageId, threadId, position);
            return position != null ? position : 0;
        } catch (Exception e) {
            log.error("[ThreadLock] Error enqueueing message for thread {}: {}", threadId, e.getMessage());
            return 0;
        }
    }

    /**
     * Get the next message from the thread's processing queue.
     * @param threadId The thread ID
     * @return The next message ID, or null if queue is empty
     */
    public String dequeue(UUID threadId) {
        String key = queueKey(threadId);
        try {
            return redisTemplate.opsForList().leftPop(key);
        } catch (Exception e) {
            log.error("[ThreadLock] Error dequeuing message for thread {}: {}", threadId, e.getMessage());
            return null;
        }
    }

    /**
     * Get thread lock status info including queue position
     * @param threadId The thread ID
     * @return ThreadLockStatus with current state
     */
    public ThreadLockStatus getStatus(UUID threadId) {
        boolean locked = isLocked(threadId);
        long queuePosition = getQueuePosition(threadId);
        return new ThreadLockStatus(locked, queuePosition);
    }

    private String lockKey(UUID threadId) {
        return String.format(LOCK_KEY_FMT, threadId);
    }

    private String queueKey(UUID threadId) {
        return String.format(QUEUE_KEY_FMT, threadId);
    }

    /**
     * Record holding thread lock status information
     */
    public record ThreadLockStatus(boolean isLocked, long queuePosition) {
        /**
         * @return true if the thread is currently processing a message
         */
        public boolean isBusy() {
            return isLocked;
        }

        /**
         * @return number of messages waiting in queue (including current if locked)
         */
        public long getWaitingCount() {
            return queuePosition;
        }
    }
}


