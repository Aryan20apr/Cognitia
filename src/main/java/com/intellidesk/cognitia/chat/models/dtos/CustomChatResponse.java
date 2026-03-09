package com.intellidesk.cognitia.chat.models.dtos;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class CustomChatResponse {
    
    private String answer;

    @JsonAlias({"sources", "references"})
    private List<String> references;

    @JsonAlias({"followUpSuggestions", "suggestedActions"})
    private List<String> suggestedActions;
}
