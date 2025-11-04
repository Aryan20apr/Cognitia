package com.intellidesk.cognitia.analytics.utils;

import org.springframework.stereotype.Component;

import com.intellidesk.cognitia.analytics.service.RedisCounterService;

import java.time.Duration;

// @Component
public class RequestIdGuard {

    private final RedisCounterService redisCounterService;
    private static final Duration REQUEST_TTL = Duration.ofHours(6);

    public RequestIdGuard(RedisCounterService redisCounterService) {
        this.redisCounterService = redisCounterService;
    }

    public boolean acquire(String requestId) {
        if (requestId == null) return true; // no id => best-effort, not idempotent
        return redisCounterService.tryAcquireRequestId(requestId, REQUEST_TTL);
    }
}

