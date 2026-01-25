package com.intellidesk.cognitia.payments.models.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(
    name = "payments",
    indexes = {
        @Index(name = "idx_pay_event_payment_id", columnList = "payment_id"),
        @Index(name = "idx_pay_event_order_id", columnList = "order_id"),
        @Index(name = "idx_pay_event_status", columnList = "status"),
        @Index(name = "idx_pay_event_received_at", columnList = "event_received_at"),
        @Index(name = "idx_pay_event_event_type", columnList = "event_type")
    },
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_pay_event_idempotency",
            columnNames = "idempotency_key"
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    /* -----------------------------
     * Internal identity
     * ----------------------------- */

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(nullable = false, updatable = false)
    private UUID id;

    /* -----------------------------
     * Razorpay identifiers
     * ----------------------------- */

    @Column(name = "payment_id", nullable = false, length = 50)
    private String paymentId;

    @Column(name = "order_id", nullable = false, length = 50)
    private String orderId;

    /* -----------------------------
     * Event semantics
     * ----------------------------- */

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType; 
    // payment.authorized | payment.captured | payment.failed | payment.refunded

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "event_received_at", nullable = false)
    private OffsetDateTime eventReceivedAt;

    /* -----------------------------
     * Monetary snapshot
     * ----------------------------- */

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "amount_refunded")
    private Long amountRefunded;

    @Column(name = "fee")
    private Long fee;

    @Column(name = "tax")
    private Long tax;

    /* -----------------------------
     * Payment attributes
     * ----------------------------- */

    @Column(name = "method", length = 20)
    private String method;

    @Column(name = "captured")
    private Boolean captured;

    /* -----------------------------
     * Failure diagnostics
     * ----------------------------- */

    @Column(name = "error_code", length = 50)
    private String errorCode;

    @Column(name = "error_description")
    private String errorDescription;

    /* -----------------------------
     * Idempotency + evidence
     * ----------------------------- */

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;
    // e.g. Razorpay event_id

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> rawPayload;

    /* -----------------------------
     * Persistence timestamp
     * ----------------------------- */

    @Column(name = "persisted_at", nullable = false)
    private OffsetDateTime persistedAt;

    @PrePersist
    void onPersist() {
        persistedAt = OffsetDateTime.now();
    }
}

