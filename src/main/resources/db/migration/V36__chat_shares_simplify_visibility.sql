-- =============================================================================
-- V36: Replace the visibility enum with a simple tenant_wide boolean
-- =============================================================================
-- Old model: NAMED / ROLE_BASED / TENANT_WIDE / REVOKED enum
-- New model:
--   tenant_wide  boolean  — true  → anyone in the tenant can view (no grants needed)
--                         — false → access is restricted to explicit grants
--   revocation is still tracked via revoked_at / revoked_by (unchanged)
-- Grants are always mixed: user grants and role grants can coexist on the same share.
-- =============================================================================

ALTER TABLE public.chat_shares
    ADD COLUMN tenant_wide boolean NOT NULL DEFAULT false;

-- Migrate existing data: TENANT_WIDE rows become tenant_wide = true
UPDATE public.chat_shares
    SET tenant_wide = true
    WHERE visibility = 'TENANT_WIDE';

ALTER TABLE public.chat_shares
    DROP COLUMN visibility;
