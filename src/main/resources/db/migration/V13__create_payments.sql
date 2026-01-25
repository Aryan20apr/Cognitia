CREATE TABLE payments (
    id                  UUID PRIMARY KEY,

    payment_id          VARCHAR(50) NOT NULL,
    order_id            VARCHAR(50) NOT NULL,

    event_type          VARCHAR(50) NOT NULL,
    status              VARCHAR(20) NOT NULL,
    event_received_at   TIMESTAMPTZ NOT NULL,

    amount              BIGINT NOT NULL,
    currency            CHAR(3) NOT NULL,
    amount_refunded     BIGINT,
    fee                 BIGINT,
    tax                 BIGINT,

    method              VARCHAR(20),
    captured            BOOLEAN,

    error_code          VARCHAR(50),
    error_description   TEXT,

    idempotency_key     VARCHAR(100) NOT NULL,
    raw_payload         JSONB NOT NULL,

    persisted_at        TIMESTAMPTZ NOT NULL
);

-- Idempotency guarantee
CREATE UNIQUE INDEX uk_payments_idempotency_key
    ON payments(idempotency_key);

-- Query indexes
CREATE INDEX idx_pay_event_payment_id
    ON payments(payment_id);

CREATE INDEX idx_pay_event_order_id
    ON payments(order_id);

CREATE INDEX idx_pay_event_status
    ON payments(status);

CREATE INDEX idx_pay_event_event_type
    ON payments(event_type);

CREATE INDEX idx_pay_event_received_at
    ON payments(event_received_at);
