package com.intellidesk.cognitia.chat.models.dtos;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatThreadDTO {
    private UUID id;
    private String title;
    private UUID userId;
    private Instant createdAt;
    private List<ChatMessageDTO> messages;
}
