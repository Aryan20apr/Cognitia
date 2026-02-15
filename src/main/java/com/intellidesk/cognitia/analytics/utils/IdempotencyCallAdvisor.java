package com.intellidesk.cognitia.analytics.utils;

import java.util.Objects;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.stereotype.Component;

import com.intellidesk.cognitia.analytics.service.RedisIdempotencyService;
import com.intellidesk.cognitia.common.Constants;
import com.intellidesk.cognitia.utils.exceptionHandling.DuplicateRequestAlreadyProcessedException;
import com.intellidesk.cognitia.utils.exceptionHandling.DuplicateRequestInProgressException;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SignalType;

@Component
@Slf4j
public class IdempotencyCallAdvisor implements CallAdvisor, StreamAdvisor {

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

    /**
     * Common idempotency check logic - returns requestId if lock acquired, null if no requestId provided.
     * Uses tryAcquire (SETNX) as the single atomic check — no redundant peek() before it.
     */
    private String acquireIdempotencyLock(ChatClientRequest request) {
        Object reqIdObj = request.context().get(Constants.PARAM_REQUEST_ID);
        String requestId = reqIdObj == null ? null : String.valueOf(reqIdObj);

        if (requestId == null || requestId.isBlank()) {
            log.debug("[Idempotency] No requestId provided. Proceeding without idempotency check.");
            return null;
        }

        // Try to acquire processing lock atomically (SETNX)
        boolean acquired = idempotencyService.tryAcquire(requestId);
        log.info("[Idempotency] Lock acquisition for requestId [{}]: {}", requestId, acquired ? "ACQUIRED" : "FAILED");

        if (!acquired) {
            // Lock not acquired — check why (already processed vs still processing)
            String state = idempotencyService.peek(requestId);
            if (Objects.equals(state, "processed")) {
                log.info("[Idempotency] Request [{}] already processed.", requestId);
                throw new DuplicateRequestAlreadyProcessedException(requestId);
            } else {
                log.info("[Idempotency] Request [{}] is currently being processed.", requestId);
                throw new DuplicateRequestInProgressException(requestId);
            }
        }

        return requestId;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        String requestId = acquireIdempotencyLock(request);

        try {
            ChatClientResponse response = chain.nextCall(request);
            
            // Mark as processed on success
            if (requestId != null) {
                idempotencyService.markProcessed(requestId);
                log.info("[Idempotency] Request [{}] marked as processed.", requestId);
            }
            
            return response;
        } catch (Exception e) {
            // Release lock on error so request can be retried
            if (requestId != null) {
                idempotencyService.release(requestId);
                log.info("[Idempotency] Lock released for requestId [{}] due to error.", requestId);
            }
            throw e;
        }
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        String requestId = acquireIdempotencyLock(request);

        return chain.nextStream(request)
            .doFinally(signal -> {
                if (requestId == null) return;
                
                if (signal == SignalType.ON_COMPLETE) {
                    idempotencyService.markProcessed(requestId);
                    log.info("[Idempotency] Stream request [{}] marked as processed.", requestId);
                } else {
                    // ON_ERROR or CANCEL — release lock so the request can be retried
                    idempotencyService.release(requestId);
                    log.info("[Idempotency] Lock released for stream requestId [{}] (signal: {})", requestId, signal);
                }
            });
    }
}
