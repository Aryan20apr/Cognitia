

ALTER TABLE orders
    ADD COLUMN raw_order jsonb NOT NULL DEFAULT '{}'::jsonb;