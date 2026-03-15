-- =============================================================================
-- V20: Add new permissions for authorization gaps
-- =============================================================================
-- Adds permissions for chat, ingestion, orders, payments, analytics, 
-- tenant listing, and quota management.
-- =============================================================================

INSERT INTO public.permission (name, created_at, updated_at)
SELECT name, NOW(), NOW()
FROM (VALUES
    ('CHAT_ACCESS'),
    ('INGESTION_READ'),
    ('ORDER_CREATE'),
    ('ORDER_READ'),
    ('PAYMENT_VERIFY'),
    ('ANALYTICS_READ'),
    ('TENANT_LIST'),
    ('QUOTA_READ'),
    ('QUOTA_UPDATE')
) AS new_perms(name)
WHERE NOT EXISTS (
    SELECT 1 FROM public.permission p WHERE p.name = new_perms.name
);
