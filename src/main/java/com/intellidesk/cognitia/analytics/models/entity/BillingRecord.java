package com.intellidesk.cognitia.analytics.models.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.Filters;

import com.intellidesk.cognitia.userandauth.models.entities.TenantAwareEntity;

@Entity
@Table(name = "billing_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper=true)
@Filters(@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId"))
public class BillingRecord extends TenantAwareEntity {

    @Id
    @GeneratedValue
    private UUID invoiceId;

    private LocalDate periodStart;
    private LocalDate periodEnd;

    @OneToMany(mappedBy = "billingRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BillingUsageLine> usageLines;

    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    public enum PaymentStatus {
        PENDING,
        PAID,
        FAILED
    }
}
