package com.intellidesk.cognitia.chat.models.dtos;

import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareResponseDTO {
    private UUID id;
    private String token;
    private String url;
    private boolean isPublic;
    private String title;
    private Integer snapshotVersion;
    private Instant expiresAt;
    private Instant createdAt;
    private UUID sourceThreadId;
    private UUID ownerUserId;
}
