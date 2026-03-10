package com.intellidesk.cognitia.chat.models.dtos;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AgentStep(
    String type,
    String tool,
    String message,
    Map<String, Object> args,
    String reasoning,
    String confidence,
    String resultSummary,
    Long durationMs,
    List<SourceReference> sources,
    Instant timestamp
) {

    public static AgentStep thinking(String message) {
        return new AgentStep("thinking", null, message, null, null, null, null, null, null, Instant.now());
    }

    public static AgentStep toolStart(String tool, String message, Map<String, Object> args,
                                       String reasoning, String confidence) {
        return new AgentStep("tool-start", tool, message, args, reasoning, confidence, null, null, null, Instant.now());
    }

    public static AgentStep toolResult(String tool, String resultSummary, long durationMs,
                                        List<SourceReference> sources) {
        String message = resultSummary != null
            ? resultSummary
            : "Completed " + tool;
        List<SourceReference> refs = (sources != null && !sources.isEmpty()) ? sources : null;
        return new AgentStep("tool-result", tool, message, null, null, null, resultSummary, durationMs, refs, Instant.now());
    }

    public static AgentStep retrieving(String message) {
        return new AgentStep("retrieving", null, message, null, null, null, null, null, null, Instant.now());
    }

    public static AgentStep generating(String message) {
        return new AgentStep("generating", null, message, null, null, null, null, null, null, Instant.now());
    }

    public static AgentStep error(String tool, String message) {
        return new AgentStep("error", tool, message, null, null, null, null, null, null, Instant.now());
    }
}
