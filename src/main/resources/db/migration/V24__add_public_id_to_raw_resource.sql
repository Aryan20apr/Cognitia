ALTER TABLE raw_resource ADD COLUMN public_id VARCHAR(255);
ALTER TABLE raw_resource ADD CONSTRAINT uq_raw_resource_public_id UNIQUE (public_id);
