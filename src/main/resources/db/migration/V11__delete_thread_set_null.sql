-- =============================================================================
-- V11: Fix Chat Usage and Chat Thread Relationship
-- =============================================================================
-- Removes Set thread id to null for chat usage event when chat thread is deleted
-- =============================================================================


ALTER TABLE chat_usage_event
DROP CONSTRAINT fkfxrmaib9d0kqlflham64k9vcf;

ALTER TABLE chat_usage_event
ADD CONSTRAINT fk_chat_usage_thread
FOREIGN KEY (thread_id)
REFERENCES chat_threads(id)
ON DELETE SET NULL;