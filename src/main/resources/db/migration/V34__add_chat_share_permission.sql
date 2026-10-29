-- =============================================================================
-- V34: Add Chat Share Permission
-- =============================================================================
-- Creates PERM_CHAT_SHARE permission and grants it to SUPER_ADMIN role
-- =============================================================================

-- Insert the new permission if it doesn't already exist
INSERT INTO public.permission (name, created_at, updated_at)
SELECT 'PERM_CHAT_SHARE', NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM public.permission WHERE name = 'PERM_CHAT_SHARE'
);

-- Grant PERM_CHAT_SHARE to SUPER_ADMIN role
INSERT INTO public.role_permisson (role_id, permission_id)
SELECT r.role_id,
       (SELECT permission_id FROM public.permission WHERE name = 'PERM_CHAT_SHARE')
FROM public.roles r
WHERE r.role_name = 'SUPER_ADMIN'
ON CONFLICT (role_id, permission_id) DO NOTHING;
