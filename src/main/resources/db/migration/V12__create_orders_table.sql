
CREATE TABLE orders (
    id              UUID PRIMARY KEY,
    tenant_id       UUID NOT NULL,

    order_id        VARCHAR(50) NOT NULL,
    order_ref       VARCHAR(100) NOT NULL,

    amount          BIGINT NOT NULL,
    amount_paid     BIGINT NOT NULL,
    amount_due      BIGINT NOT NULL,

    currency        CHAR(3) NOT NULL,
    status          VARCHAR(20) NOT NULL,
    payment_status  VARCHAR(20 NOT NULL,
    attempts        INTEGER NOT NULL,

    notes           JSONB,

    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL
);

-- Uniqueness
CREATE UNIQUE INDEX uk_orders_order_id
    ON orders(order_id);

-- Indexes
CREATE INDEX idx_orders_merchant_order_ref
    ON orders(order_ref);

CREATE INDEX idx_orders_order_id
    ON orders(order_id);

CREATE INDEX idx_orders_status
    ON orders(status);

CREATE INDEX idx_orders_created_at
    ON orders(created_at);

-- Multi-tenant safety
CREATE INDEX idx_orders_tenant_id
    ON orders(tenant_id);
