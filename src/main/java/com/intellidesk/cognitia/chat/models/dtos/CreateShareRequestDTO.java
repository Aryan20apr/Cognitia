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
public class CreateShareRequestDTO {
    @Builder.Default
    private boolean isPublic = false;
    private List<UUID> granteeUserIds;
    private List<Integer> granteeRoleIds;
    private Instant expiresAt;
}
