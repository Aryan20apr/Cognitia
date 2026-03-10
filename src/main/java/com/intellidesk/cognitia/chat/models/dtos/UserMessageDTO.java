package com.intellidesk.cognitia.chat.models.dtos;

import java.util.List;

import lombok.Data;

@Data
public class UserMessageDTO {
    
    String message;
    String threadId;
    String requestId;
    List<String> tools;
}
