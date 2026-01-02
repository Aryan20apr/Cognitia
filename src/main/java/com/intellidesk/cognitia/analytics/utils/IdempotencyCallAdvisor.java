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
     * Common idempotency check logic - returns requestId if lock acquired, null if no requestId provided
     */
    private String acquireIdempotencyLock(ChatClientRequest request) {
        Object reqIdObj = request.context().get(Constants.PARAM_REQUEST_ID);
        String requestId = reqIdObj == null ? null : String.valueOf(reqIdObj);

        if (requestId == null || requestId.isBlank()) {
            log.info("No requestId provided in context. Proceeding without idempotency check.");
            return null;
        }

        // Quick check in Redis
        String state = idempotencyService.peek(requestId);
        log.info("Idempotency check for requestId [{}]: Redis state [{}]", requestId, state);
        
        if (Objects.equals(state, "processed")) {
            log.info("Request [{}] already processed.", requestId);
            throw new DuplicateRequestAlreadyProcessedException(requestId);
        }
        if (Objects.equals(state, "processing")) {
            log.info("Request [{}] is currently being processed.", requestId);
            throw new DuplicateRequestInProgressException(requestId);
        }

        // Try to acquire processing lock
        boolean acquired = idempotencyService.tryAcquire(requestId);
        log.info("Lock acquisition for requestId [{}]: {}", requestId, acquired ? "ACQUIRED" : "FAILED");
        
        if (!acquired) {
            state = idempotencyService.peek(requestId);
            if (Objects.equals(state, "processed")) {
                throw new DuplicateRequestAlreadyProcessedException(requestId);
            } else {
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
                log.info("Request [{}] marked as processed.", requestId);
            }
            
            return response;
        } catch (Exception e) {
            // Release lock on error so request can be retried
            if (requestId != null) {
                idempotencyService.release(requestId);
                log.info("Lock released for requestId [{}] due to error.", requestId);
            }
            throw e;
        }
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        String requestId = acquireIdempotencyLock(request);

        return chain.nextStream(request)
            .doOnComplete(() -> {
                if (requestId != null) {
                    idempotencyService.markProcessed(requestId);
                    log.info("Stream request [{}] marked as processed.", requestId);
                }
            })
            .doOnError(e -> {
                if (requestId != null) {
                    idempotencyService.release(requestId);
                    log.info("Lock released for stream requestId [{}] due to error: {}", requestId, e.getMessage());
                }
            });
    }
}
