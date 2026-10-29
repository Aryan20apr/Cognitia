package com.intellidesk.cognitia.chat.models.dtos;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareUserSummaryDTO {
    private UUID id;
    private String name;
    private String email;
}
