package com.intellidesk.cognitia.chat.models.dtos;

import lombok.Data;

@Data
public class UserMessageDTO {
    
    String message;
    String threadId;
    String requestId;
}
