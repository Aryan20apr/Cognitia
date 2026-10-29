package com.intellidesk.cognitia.chat.models.dtos;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharedThreadViewDTO {
    private UUID shareId;
    private String title;
    private String ownerName;
    private Instant createdAt;
    private List<ChatMessageDTO> messages;
}
