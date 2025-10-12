package com.intellidesk.cognitia.chat.models.dtos;

import java.util.List;

import lombok.Data;

@Data
public class CustomChatResponse {
    
    private String answer;
    private List<String> references;
    private List<String> suggestedActions;
}
