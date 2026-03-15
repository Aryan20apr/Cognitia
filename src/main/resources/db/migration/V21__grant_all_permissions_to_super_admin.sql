-- =============================================================================
-- V21: Grant all permissions to every SUPER_ADMIN role
-- =============================================================================
-- Ensures that all SUPER_ADMIN roles (across all tenants) have every
-- permission in the system
-- Uses INSERT ... ON CONFLICT DO NOTHING to be idempotent.
-- =============================================================================

INSERT INTO public.role_permisson (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM public.roles r
CROSS JOIN public.permission p
WHERE r.role_name = 'SUPER_ADMIN'
ON CONFLICT (role_id, permission_id) DO NOTHING;
