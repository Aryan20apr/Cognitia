package com.intellidesk.cognitia.analytics.models.dto;


import java.math.BigDecimal;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Plan catalog DTO")
public class PlanDTO {
    private UUID id;
    private String code;
    private String name;
    private String description;
    private Long includedPromptTokens;
    private Long includedCompletionTokens;
    private Long includedTotalTokens;
    private Long includedDocs;
    private Long includedUsers;
    private BigDecimal pricePerMonth;
    private String overagePer1KTokens;
    private Boolean trialAvailable;
    private Integer trialDays;
    private String modelRestriction;
}
