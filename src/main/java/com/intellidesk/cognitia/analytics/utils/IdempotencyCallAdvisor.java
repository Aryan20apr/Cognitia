package com.intellidesk.cognitia.analytics.utils;



import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.stereotype.Component;

import com.intellidesk.cognitia.analytics.service.RedisIdempotencyService;
import com.intellidesk.cognitia.common.Constants;
import com.intellidesk.cognitia.utils.exceptionHandling.DuplicateRequestAlreadyProcessedException;
import com.intellidesk.cognitia.utils.exceptionHandling.DuplicateRequestInProgressException;

import java.util.Objects;

@Component
public class IdempotencyCallAdvisor implements CallAdvisor {

    private final RedisIdempotencyService idempotencyService;

    public IdempotencyCallAdvisor(RedisIdempotencyService idempotencyService) {
        this.idempotencyService = idempotencyService;
    }

    @Override
    public String getName() {
        return "IdempotencyCallAdvisor";
    }

    @Override
    public int getOrder() {
        return -200; // very early — ensure acquisition happens before expensive advisors
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {

        Object reqIdObj = request.context().get(Constants.PARAM_REQUEST_ID);
        String requestId = reqIdObj == null ? null : String.valueOf(reqIdObj);

        // If no requestId was provided, allow to proceed (can't guard).
        if (requestId == null || requestId.isBlank()) {
            return chain.nextCall(request);
        }

        // 1) Quick check in Redis
        String state = idempotencyService.peek(requestId);
        if (Objects.equals(state, "processed")) {
            // Already processed — throw or return cached
            throw new DuplicateRequestAlreadyProcessedException(requestId);
        }
        if (Objects.equals(state, "processing")) {
            // Another worker is currently processing same request
            throw new DuplicateRequestInProgressException(requestId);
        }

        // 2) Try to acquire processing lock
        boolean acquired = idempotencyService.tryAcquire(requestId);
        if (!acquired) {
            // Race: maybe someone obtained it; peek again
            state = idempotencyService.peek(requestId);
            if (Objects.equals(state, "processed")) {
                throw new DuplicateRequestAlreadyProcessedException(requestId);
            } else {
                throw new DuplicateRequestInProgressException(requestId);
            }
        }

        // If acquired, proceed with chain; do NOT release here — markProcessed will be called by aggregator
        return chain.nextCall(request);
    }
}
