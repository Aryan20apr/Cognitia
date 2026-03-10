package com.intellidesk.cognitia.chat.service.tools;

import java.util.List;

import com.intellidesk.cognitia.chat.models.dtos.SourceReference;

public interface TimelineAwareTool {

 
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
