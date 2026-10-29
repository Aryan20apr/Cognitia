-- =============================================================================
-- V35: Enforce one share per thread
-- =============================================================================
-- A thread has exactly one share record. The token on that record is the
-- stable URL — it never changes unless the owner explicitly rotates it.
-- =============================================================================

ALTER TABLE public.chat_shares
    ADD CONSTRAINT uq_chat_shares_source_thread UNIQUE (source_thread_id);
