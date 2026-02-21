package com.intellidesk.cognitia.chat.service.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.augment.AugmentedToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellidesk.cognitia.chat.models.dtos.AgentStep;
import com.intellidesk.cognitia.chat.service.AgentTimelineContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class TimelineToolCallbackProvider {

    private final ObjectMapper objectMapper;

    private static final Map<String, String> TOOL_DESCRIPTIONS = Map.of(
        "searchWeb", "Searching the web",
        "extractText", "Reading web page",
        "getCurrentDateTime", "Checking current time"
    );

    /**
     * Creates per-request augmented tool callbacks that capture the given timeline
     * context directly in their closures. 
     */
    public ToolCallback[] createAugmentedToolCallbacks(AgentTimelineContext timeline, Object... toolObjects) {
        List<ToolCallback> allCallbacks = new ArrayList<>();

        for (Object toolObject : toolObjects) {
            AugmentedToolCallbackProvider<AgentThinking> augmented =
                AugmentedToolCallbackProvider.<AgentThinking>builder()
                    .toolObject(toolObject)
                    .argumentType(AgentThinking.class)
                    .argumentConsumer(event -> {
                        if (timeline == null) return;

                        String toolName = event.toolDefinition().name();
                        AgentThinking thinking = event.arguments();

                        Map<String, Object> args = parseArgs(event.rawInput());

                        timeline.emitStep(AgentStep.toolStart(
                            toolName,
                            TOOL_DESCRIPTIONS.getOrDefault(toolName, "Using " + toolName + "..."),
                            args,
                            thinking != null ? thinking.innerThought() : null,
                            thinking != null ? thinking.confidence() : null
                        ));
                    })
                    .removeExtraArgumentsAfterProcessing(true)
                    .build();

            ToolCallback[] callbacks = augmented.getToolCallbacks();
            for (ToolCallback cb : callbacks) {
                allCallbacks.add(new TimedToolCallback(cb, timeline));
            }
        }

        return allCallbacks.toArray(ToolCallback[]::new);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseArgs(String rawInput) {
        if (rawInput == null || rawInput.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(rawInput, Map.class);
        } catch (Exception e) {
            return Map.of("raw", rawInput);
        }
    }

    static class TimedToolCallback implements ToolCallback {

        private final ToolCallback delegate;
        private final AgentTimelineContext timeline;

        TimedToolCallback(ToolCallback delegate, AgentTimelineContext timeline) {
            this.delegate = delegate;
            this.timeline = timeline;
        }

        @Override
        public ToolDefinition getToolDefinition() {
            return delegate.getToolDefinition();
        }

        @Override
        public ToolMetadata getToolMetadata() {
            return delegate.getToolMetadata();
        }

        @Override
        public String call(String toolInput) {
            String toolName = delegate.getToolDefinition().name();
            long start = System.currentTimeMillis();

            try {
                String result = delegate.call(toolInput);
                long duration = System.currentTimeMillis() - start;

                if (timeline != null) {
                    timeline.emitStep(AgentStep.toolResult(
                        toolName,
                        summarizeResult(toolName, result),
                        duration
                    ));
                }
                return result;
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - start;

                if (timeline != null) {
                    timeline.emitStep(AgentStep.error(toolName,
                        "Failed after " + duration + "ms: " + e.getMessage()));
                }
                throw e;
            }
        }

        @Override
        public String call(String toolInput, ToolContext toolContext) {
            return call(toolInput);
        }

        private String summarizeResult(String toolName, String result) {
            if (result == null) return "No result";

            return switch (toolName) {
                case "searchWeb" -> {
                    int count = countOccurrences(result, "\"url\"");
                    yield "Found " + count + " search result" + (count != 1 ? "s" : "");
                }
                case "extractText" -> {
                    int sources = countOccurrences(result, "## Source:");
                    yield "Extracted content from " + sources + " page" + (sources != 1 ? "s" : "");
                }
                case "getCurrentDateTime" -> "Got current time: " + truncate(result, 30);
                default -> truncate(result, 80);
            };
        }

        private int countOccurrences(String text, String pattern) {
            int count = 0;
            int idx = 0;
            while ((idx = text.indexOf(pattern, idx)) != -1) {
                count++;
                idx += pattern.length();
            }
            return Math.max(count, 1);
        }

        private String truncate(String text, int maxLen) {
            if (text.length() <= maxLen) return text;
            return text.substring(0, maxLen) + "...";
        }
    }
}
