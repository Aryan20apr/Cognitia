package com.intellidesk.cognitia.chat.service.tools;

import java.time.LocalDateTime;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class DateTimeTool implements TimelineAwareTool {

    @Tool(description = "Get the current date and time in the user's timezone")
    String getCurrentDateTime() {
        
        log.info("Fetching current date and time for timezone: {}", LocaleContextHolder.getTimeZone().getID());
        return LocalDateTime.now().atZone(LocaleContextHolder.getTimeZone().toZoneId()).toString();
    }

    @Override
    public String timelineDescription(){
        return "Getting current date and time";
    }

    @Override
    public String summarizeResult(String rawJsonResult) {
        if (rawJsonResult == null || rawJsonResult.isBlank()) return "Time retrieved";
        String display = rawJsonResult.length() > 30 ? rawJsonResult.substring(0, 30) + "..." : rawJsonResult;
        return "Current time: " + display;
    }

}
