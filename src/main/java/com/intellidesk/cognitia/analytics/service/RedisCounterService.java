package com.intellidesk.cognitia.analytics.service;


import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;


import com.intellidesk.cognitia.common.Constants;

import java.time.Duration;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class RedisCounterService {

    private final StringRedisTemplate redisTemplate;
    private static final DateTimeFormatter YM = DateTimeFormatter.ofPattern("yyyy-MM");

    public RedisCounterService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String tenantTokensKey(UUID tenantId) {
        String ym = YearMonth.now().format(YM);
        return String.format(Constants.REDIS_TENANT_TOKEN_KEY_FMT, tenantId.toString(), ym);
    }

    public String userTokensKey(UUID tenantId, UUID userId) {
        String ym = YearMonth.now().format(YM);
        return String.format(Constants.REDIS_USER_TOKEN_KEY_FMT, tenantId.toString(), userId.toString(), ym);
    }

    /** Atomic increment for tenant tokens. Returns new value. */
    public long incrementTenantTokens(UUID tenantId, long delta) {
        String key = tenantTokensKey(tenantId);
        Long val = redisTemplate.opsForValue().increment(key, delta);
        // set TTL for one year if newly created
        if (val != null && val == delta) {
            redisTemplate.expire(key, Duration.ofDays(400)); // keep a while
        }
        return val == null ? 0 : val;
    }

    /** Atomic increment for user tokens. Returns new value. */
    public long incrementUserTokens(UUID tenantId, UUID userId, long delta) {
        String key = userTokensKey(tenantId, userId);
        Long val = redisTemplate.opsForValue().increment(key, delta);
        if (val != null && val == delta) {
            redisTemplate.expire(key, Duration.ofDays(400));
        }
        return val == null ? 0 : val;
    }

    /** Idempotency guard for requestId. Returns true if successfully set, false if already processed. */
    public boolean tryAcquireRequestId(String requestId, Duration ttl) {
        String key = String.format(Constants.REDIS_REQUEST_ID_KEY_FMT, requestId);
        Boolean success = redisTemplate.opsForValue().setIfAbsent(key, "1", ttl);
        return success != null && success;
    }

    public String getValue(String key){
        return redisTemplate.opsForValue().get(key);
    }
}
