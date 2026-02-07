-- Add payment verification and purpose tracking columns to orders table
-- These columns support payment verification and plan upgrade payment verification features


ALTER TABLE orders ADD COLUMN IF NOT EXISTS payment_verfication VARCHAR(20);


ALTER TABLE orders ADD COLUMN IF NOT EXISTS purpose_type VARCHAR(50);


ALTER TABLE orders ADD COLUMN IF NOT EXISTS purpose_ref_id UUID;


ALTER TABLE orders ADD COLUMN IF NOT EXISTS fulfillment_status VARCHAR(20) DEFAULT 'UNFULFILLED';


CREATE INDEX IF NOT EXISTS idx_orders_purpose_fulfillment 
    ON orders(tenant_id, purpose_type, purpose_ref_id, payment_verfication, fulfillment_status);

CREATE INDEX IF NOT EXISTS idx_orders_payment_verification 
    ON orders(payment_verfication);
