package com.intellidesk.cognitia.analytics.models.dto;


import java.util.Date;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Detailed chat usage record")
public class ChatUsageDetailsDTO {

    private UUID id;
    private UUID userId;
    private UUID tenantId;
    private UUID threadId;
    private String requestId;
    private Long promptTokens;
    private Long completionTokens;
    private Long totalTokens;
    private Boolean isProcessed;
    private Double estimatedCost;
    private String modelName;
    private Long latencyMs;
    private String metaDataJson;
    private Date createdAt;
    private Date updatedAt;
}
