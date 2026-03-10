package com.intellidesk.cognitia.chat.service.tools;

import java.util.List;

import com.intellidesk.cognitia.chat.models.dtos.SourceReference;

public interface TimelineAwareTool {

    default String toolId() {
        return null;
    }

    default String displayName() {
        return getClass().getSimpleName();
    }

    default String category() {
        return "general";
    }

    default boolean userSelectable() {
        return true;
    }

    default String timelineDescription() {
        return null;
    }

    default String summarizeResult(String rawJsonResult) {
        return null;
    }

    default List<SourceReference> extractSources(String rawJsonResult) {
        return List.of();
    }
}
