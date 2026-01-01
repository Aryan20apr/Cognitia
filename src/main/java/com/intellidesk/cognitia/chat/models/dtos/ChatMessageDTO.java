package com.intellidesk.cognitia.chat.models.dtos;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatMessageDTO {
    private UUID id;
    private String content;
    private String role;
    private Instant createdAt;
}
