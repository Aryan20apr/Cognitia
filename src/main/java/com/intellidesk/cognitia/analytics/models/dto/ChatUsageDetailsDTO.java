package com.intellidesk.cognitia.analytics.models.dto;


import java.util.Date;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatUsageDetailsDTO {

    private UUID id;
    private UUID userId;
    private UUID tenantId;
    private UUID threadId;

    private Long promptTokens;
    private Long completionTokens;
    private Long totalTokens;

    private Double estimatedCost;
    private String modelName;
    private Long latencyMs;

    private Date createdAt;
    private Date updatedAt;
}
