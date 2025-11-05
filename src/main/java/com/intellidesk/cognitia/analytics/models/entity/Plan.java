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

    @Column(nullable = true, unique = false)
    private String code; // e.g., "starter", "pro", "enterprise"

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

    private Boolean active = Boolean.TRUE;

    // metadata json or additional attributes
    @Column(columnDefinition = "text")
    private String metadata;

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Plan other = (Plan) obj;
        if (code == null) {
            if (other.code != null)
                return false;
        } else if (!code.equals(other.code))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (includedPromptTokens == null) {
            if (other.includedPromptTokens != null)
                return false;
        } else if (!includedPromptTokens.equals(other.includedPromptTokens))
            return false;
        if (includedCompletionTokens == null) {
            if (other.includedCompletionTokens != null)
                return false;
        } else if (!includedCompletionTokens.equals(other.includedCompletionTokens))
            return false;
        if (includedTotalTokens == null) {
            if (other.includedTotalTokens != null)
                return false;
        } else if (!includedTotalTokens.equals(other.includedTotalTokens))
            return false;
        if (includedDocs == null) {
            if (other.includedDocs != null)
                return false;
        } else if (!includedDocs.equals(other.includedDocs))
            return false;
        if (includedUsers == null) {
            if (other.includedUsers != null)
                return false;
        } else if (!includedUsers.equals(other.includedUsers))
            return false;
        if (pricePerMonth == null) {
            if (other.pricePerMonth != null)
                return false;
        } else if (!pricePerMonth.equals(other.pricePerMonth))
            return false;
        if (overagePricing == null) {
            if (other.overagePricing != null)
                return false;
        } else if (!overagePricing.equals(other.overagePricing))
            return false;
        if (modelRestrictions == null) {
            if (other.modelRestrictions != null)
                return false;
        } else if (!modelRestrictions.equals(other.modelRestrictions))
            return false;
        if (active == null) {
            if (other.active != null)
                return false;
        } else if (!active.equals(other.active))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((code == null) ? 0 : code.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((includedPromptTokens == null) ? 0 : includedPromptTokens.hashCode());
        result = prime * result + ((includedCompletionTokens == null) ? 0 : includedCompletionTokens.hashCode());
        result = prime * result + ((includedTotalTokens == null) ? 0 : includedTotalTokens.hashCode());
        result = prime * result + ((includedDocs == null) ? 0 : includedDocs.hashCode());
        result = prime * result + ((includedUsers == null) ? 0 : includedUsers.hashCode());
        result = prime * result + ((pricePerMonth == null) ? 0 : pricePerMonth.hashCode());
        result = prime * result + ((overagePricing == null) ? 0 : overagePricing.hashCode());
        result = prime * result + ((modelRestrictions == null) ? 0 : modelRestrictions.hashCode());
        result = prime * result + ((active == null) ? 0 : active.hashCode());
        return result;
    }

    
}
