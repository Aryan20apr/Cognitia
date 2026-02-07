-- Add processing status and retry tracking columns to payments table
-- These columns support the outbox pattern for reliable webhook processing

-- Processing status to track webhook processing state
ALTER TABLE payments ADD COLUMN IF NOT EXISTS processing_status VARCHAR(20) NOT NULL DEFAULT 'PENDING';

-- Retry tracking
ALTER TABLE payments ADD COLUMN IF NOT EXISTS attempts INTEGER NOT NULL DEFAULT 0;
ALTER TABLE payments ADD COLUMN IF NOT EXISTS max_attempts INTEGER NOT NULL DEFAULT 5;
ALTER TABLE payments ADD COLUMN IF NOT EXISTS last_error VARCHAR(1000);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS processed_at TIMESTAMPTZ;

-- Index for retry queries (find failed/pending payments)
CREATE INDEX IF NOT EXISTS idx_payments_processing_status 
    ON payments(processing_status);

-- Composite index for retry scheduler queries
CREATE INDEX IF NOT EXISTS idx_payments_retry_lookup 
    ON payments(processing_status, attempts, persisted_at);

-- Update existing rows to COMPLETED if they exist (they were processed before this migration)
UPDATE payments SET processing_status = 'COMPLETED', processed_at = persisted_at 
    WHERE processing_status = 'PENDING';

COMMENT ON COLUMN payments.processing_status IS 'PENDING=received not processed, PROCESSING=in progress, COMPLETED=done, FAILED=needs retry';
COMMENT ON COLUMN payments.attempts IS 'Number of processing attempts made';
COMMENT ON COLUMN payments.max_attempts IS 'Maximum retry attempts before giving up';
COMMENT ON COLUMN payments.last_error IS 'Last error message if processing failed';
COMMENT ON COLUMN payments.processed_at IS 'Timestamp when processing completed successfully';
