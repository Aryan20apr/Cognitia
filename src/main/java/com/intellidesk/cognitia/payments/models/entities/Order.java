package com.intellidesk.cognitia.payments.models.entities;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.Filters;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.intellidesk.cognitia.userandauth.models.entities.TenantAwareEntity;
import com.intellidesk.cognitia.utils.uuidv7.GeneratedUuidV7;

import jakarta.persistence.Column;
import jakarta.persistence.Table;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper=true)
@Filters(@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId"))
@Table(
    name = "orders",
    indexes = {
        @Index(
            name = "idx_orders_merchant_order_ref",
            columnList = "order_ref"
        ),
        @Index(
            name = "idx_orders_razorpay_order_id",
            columnList = "order_id"
        ),
        @Index(
            name = "idx_orders_status",
            columnList = "status"
        ),
        @Index(
            name = "idx_orders_created_at",
            columnList = "created_at"
        )
    },
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_orders_razorpay_order_id",
            columnNames = "order_id"
        )
    }
)

public class Order extends TenantAwareEntity {

    @Id
    @GeneratedUuidV7
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "order_id", nullable = false, length = 50)
    private String orderId;

    @Column(name = "order_ref", nullable = false, length = 100)
    private String orderRef;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "amount_paid", nullable = false)
    private Long amountPaid;

    @Column(name = "amount_due", nullable = false)
    private Long amountDue;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "attempts", nullable = false)
    private Integer attempts;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "notes", columnDefinition = "jsonb")
    private Map<String, Object> notes;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}