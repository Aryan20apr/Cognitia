package com.intellidesk.cognitia.chat.service;

import org.springframework.http.codec.ServerSentEvent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.intellidesk.cognitia.chat.models.dtos.AgentStep;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Slf4j
public class AgentTimelineContext {

    private final Sinks.Many<ServerSentEvent<String>> sink;
    private final ObjectMapper objectMapper;

    public AgentTimelineContext() {
        this.sink = Sinks.many().multicast().onBackpressureBuffer();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public void emitStep(AgentStep step) {
        try {
            String json = objectMapper.writeValueAsString(step);
            Sinks.EmitResult result = sink.tryEmitNext(
                ServerSentEvent.<String>builder(json)
                    .event("agent-step")
                    .build()
            );
            if (result.isFailure()) {
                log.warn("[Timeline] Failed to emit step: {} - {}", step.type(), result);
            } else {
                log.debug("[Timeline] Emitted step: type={}, tool={}, message={}",
                    step.type(), step.tool(), step.message());
            }
        } catch (JsonProcessingException e) {
            log.error("[Timeline] Failed to serialize AgentStep: {}", e.getMessage());
        }
    }

    public Flux<ServerSentEvent<String>> steps() {
        return sink.asFlux();
    }

    public void complete() {
        sink.tryEmitComplete();
    }
}
