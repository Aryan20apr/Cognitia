package com.intellidesk.cognitia.analytics.models.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "plans")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;
    private String description;

    @OneToMany(mappedBy = "planId")
    private Set<TenantQuota> tenantQuotas;

    private Long includedPromptTokens;
    private Long includedCompletionTokens;
    private Long includedTotalTokens;

    private Long includedDocs;
    private Long includedUsers;

    private BigDecimal pricePerMonth;

    // Stored as JSON string to support multiple overage pricing models
    @Column(columnDefinition = "TEXT")
    private String overagePricing; // e.g., {"token":0.0001,"doc":0.05}

    // Comma-separated or JSON array of model restrictions
    @Column(columnDefinition = "TEXT")
    private String modelRestrictions;
}
