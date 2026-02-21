package com.intellidesk.cognitia.chat.service.tools;

import java.util.ArrayList;
import java.util.HashMap;
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

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class TimelineToolCallbackProvider {

    private final ObjectMapper objectMapper;
    private final List<Object> allTools;
    private final Map<String, TimelineAwareTool> toolIndex;

    public TimelineToolCallbackProvider(ObjectMapper objectMapper, List<TimelineAwareTool> timelineAwareTools) {
        this.objectMapper = objectMapper;
        this.allTools = new ArrayList<>(timelineAwareTools);
        this.toolIndex = buildToolIndex(timelineAwareTools);
        log.info("TimelineToolCallbackProvider initialized with {} tools: {}", toolIndex.size(), toolIndex.keySet());
    }

    private Map<String, TimelineAwareTool> buildToolIndex(List<TimelineAwareTool> tools) {
        Map<String, TimelineAwareTool> index = new HashMap<>();
        for (TimelineAwareTool tool : tools) {
            ToolCallback[] cbs = new AugmentedToolCallbackProvider<>(tool, AgentThinking.class, e -> {}, true)
                    .getToolCallbacks();
            for (ToolCallback cb : cbs) {
                index.put(cb.getToolDefinition().name(), tool);
            }
        }
        return index;
    }

    public ToolCallback[] createAugmentedToolCallbacks(AgentTimelineContext timeline) {
        return createAugmentedToolCallbacks(timeline, allTools.toArray());
    }

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

                        TimelineAwareTool aware = toolIndex.get(toolName);
                        String description = (aware != null && aware.timelineDescription() != null)
                                ? aware.timelineDescription()
                                : "Using " + toolName + "...";

                        timeline.emitStep(AgentStep.toolStart(
                            toolName,
                            description,
                            args,
                            thinking != null ? thinking.innerThought() : null,
                            thinking != null ? thinking.confidence() : null
                        ));
                    })
                    .removeExtraArgumentsAfterProcessing(true)
                    .build();

            ToolCallback[] callbacks = augmented.getToolCallbacks();
            for (ToolCallback cb : callbacks) {
                allCallbacks.add(new TimedToolCallback(cb, timeline, toolIndex));
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
        private final Map<String, TimelineAwareTool> toolIndex;

        TimedToolCallback(ToolCallback delegate, AgentTimelineContext timeline, Map<String, TimelineAwareTool> toolIndex) {
            this.delegate = delegate;
            this.timeline = timeline;
            this.toolIndex = toolIndex;
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

            TimelineAwareTool aware = toolIndex.get(toolName);
            if (aware != null) {
                String summary = aware.summarizeResult(result);
                if (summary != null) return summary;
            }

            return truncate(result, 80);
        }

        private String truncate(String text, int maxLen) {
            if (text.length() <= maxLen) return text;
            return text.substring(0, maxLen) + "...";
        }
    }
}
