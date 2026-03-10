package com.intellidesk.cognitia.chat.service.tools;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.augment.AugmentedToolCallbackProvider;
import org.springframework.stereotype.Service;

import com.intellidesk.cognitia.chat.models.dtos.ToolDescriptor;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ToolRegistryService {

    private final Map<String, ToolCallback> callbacksByStableId = new LinkedHashMap<>();
    private final Map<String, TimelineAwareTool> nativeToolsByStableId = new LinkedHashMap<>();
    private final Map<String, String> stableIdToSpringName = new LinkedHashMap<>();
    private final Map<String, TimelineAwareTool> nativeToolsBySpringName = new LinkedHashMap<>();
    private final Map<String, String> descriptionByStableId = new LinkedHashMap<>();
    private final List<Object> allToolObjects = new ArrayList<>();

    public ToolRegistryService(
            List<TimelineAwareTool> nativeTools,
            Optional<List<ToolCallbackProvider>> mcpProviders) {

        for (TimelineAwareTool tool : nativeTools) {
            ToolCallback[] cbs = new AugmentedToolCallbackProvider<>(
                    tool, AgentThinking.class, e -> {}, true).getToolCallbacks();

            for (ToolCallback cb : cbs) {
                String springName = cb.getToolDefinition().name();
                String stableId = tool.toolId() != null ? tool.toolId() : springName;

                callbacksByStableId.put(stableId, cb);
                nativeToolsByStableId.put(stableId, tool);
                nativeToolsBySpringName.put(springName, tool);
                stableIdToSpringName.put(stableId, springName);
                descriptionByStableId.put(stableId, cb.getToolDefinition().description());
            }
            allToolObjects.add(tool);
        }

        mcpProviders.ifPresent(providers -> {
            for (ToolCallbackProvider provider : providers) {
                for (ToolCallback cb : provider.getToolCallbacks()) {
                    String springName = cb.getToolDefinition().name();
                    callbacksByStableId.put(springName, cb);
                    stableIdToSpringName.put(springName, springName);
                    descriptionByStableId.put(springName, cb.getToolDefinition().description());
                }
            }
        });

        log.info("ToolRegistryService initialized with {} tools: {}", callbacksByStableId.size(), callbacksByStableId.keySet());
    }

    public List<ToolDescriptor> getAvailableTools() {
        return callbacksByStableId.keySet().stream()
                .map(stableId -> {
                    TimelineAwareTool aware = nativeToolsByStableId.get(stableId);
                    return new ToolDescriptor(
                            stableId,
                            aware != null ? aware.displayName() : humanize(stableId),
                            descriptionByStableId.get(stableId),
                            aware != null ? aware.category() : "mcp",
                            aware != null ? "native" : "mcp",
                            aware != null ? aware.userSelectable() : true
                    );
                })
                .toList();
    }

    public List<ToolDescriptor> getUserSelectableTools() {
        return getAvailableTools().stream()
                .filter(ToolDescriptor::userSelectable)
                .toList();
    }

    public List<Object> resolveToolObjectsByStableIds(List<String> stableIds) {
        if (stableIds == null || stableIds.isEmpty()) {
            return allToolObjects;
        }
        List<Object> resolved = new ArrayList<>();

        for (TimelineAwareTool tool : nativeToolsByStableId.values()) {
            if (!tool.userSelectable()) {
                resolved.add(tool);
            }
        }

        stableIds.stream()
                .map(nativeToolsByStableId::get)
                .filter(Objects::nonNull)
                .filter(t -> !resolved.contains(t))
                .distinct()
                .forEach(resolved::add);

        return resolved;
    }

    public List<Object> getAllToolObjects() {
        return allToolObjects;
    }

    public TimelineAwareTool getNativeToolBySpringName(String springName) {
        return nativeToolsBySpringName.get(springName);
    }

    public List<String> resolveDisplayNames(List<String> stableIds) {
        if (stableIds == null || stableIds.isEmpty()) return List.of();
        return stableIds.stream()
                .map(id -> {
                    TimelineAwareTool aware = nativeToolsByStableId.get(id);
                    return aware != null ? aware.displayName() : humanize(id);
                })
                .toList();
    }

    public boolean isValidToolId(String stableId) {
        return callbacksByStableId.containsKey(stableId);
    }

    private String humanize(String name) {
        return name.replaceAll("([a-z])([A-Z])", "$1 $2")
                   .replaceAll("[-_]", " ")
                   .substring(0, 1).toUpperCase()
                   + name.replaceAll("([a-z])([A-Z])", "$1 $2")
                         .replaceAll("[-_]", " ")
                         .substring(1);
    }
}
