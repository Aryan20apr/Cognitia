package com.intellidesk.cognitia.analytics.models.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "billing_usage_lines")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingUsageLine {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billing_record_id")
    private BillingRecord billingRecord;

    private String description; // e.g. "Token Overage", "Extra Documents"
    private Long quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
}
