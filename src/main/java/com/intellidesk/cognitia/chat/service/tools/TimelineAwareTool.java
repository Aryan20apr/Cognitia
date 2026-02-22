package com.intellidesk.cognitia.chat.service.tools;


public interface TimelineAwareTool {

 
    default String timelineDescription() {
        return null;
    }


    default String summarizeResult(String rawJsonResult) {
        return null;
    }
}
