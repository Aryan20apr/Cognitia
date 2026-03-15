-- =============================================================================
-- V22: Platform Admin Architecture
-- =============================================================================
-- 1. Creates the system tenant (well-known UUID 00000000-...)
-- 2. Adds QUOTA_ADMIN permission, removes QUOTA_UPDATE
-- 3. Creates PLATFORM_ADMIN role scoped to the system tenant
-- 4. Grants all permissions to PLATFORM_ADMIN
-- 5. Revokes platform-level permissions from tenant SUPER_ADMIN roles
-- =============================================================================

-- 1. Insert system tenant
INSERT INTO public.tenants (id, name, about, domain, contact_email, created_at, updated_at)
VALUES (
    '00000000-0000-0000-0000-000000000000',
    'Cognitia Platform',
    'Internal system tenant for platform administration',
    'platform.cognitia.internal',
    'platform@cognitia.internal',
    NOW(), NOW()
)
ON CONFLICT (id) DO NOTHING;

-- 2a. Insert QUOTA_ADMIN permission
INSERT INTO public.permission (name, created_at, updated_at)
SELECT 'QUOTA_ADMIN', NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM public.permission WHERE name = 'QUOTA_ADMIN'
);

-- 2b. Remove QUOTA_UPDATE from role_permisson junction table, then from permission table
DELETE FROM public.role_permisson
WHERE permission_id IN (
    SELECT permission_id FROM public.permission WHERE name = 'QUOTA_UPDATE'
);

DELETE FROM public.permission WHERE name = 'QUOTA_UPDATE';

-- 3. Create PLATFORM_ADMIN role in the system tenant
INSERT INTO public.roles (tenant_id, role_name, created_at, updated_at)
SELECT '00000000-0000-0000-0000-000000000000', 'PLATFORM_ADMIN', NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM public.roles
    WHERE tenant_id = '00000000-0000-0000-0000-000000000000'
      AND role_name = 'PLATFORM_ADMIN'
);

-- 4. Grant ALL permissions to PLATFORM_ADMIN
INSERT INTO public.role_permisson (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM public.roles r
CROSS JOIN public.permission p
WHERE r.role_name = 'PLATFORM_ADMIN'
  AND r.tenant_id = '00000000-0000-0000-0000-000000000000'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- 5. Revoke platform-level permissions from all tenant SUPER_ADMIN roles
DELETE FROM public.role_permisson
WHERE role_id IN (
    SELECT role_id FROM public.roles WHERE role_name = 'SUPER_ADMIN'
)
AND permission_id IN (
    SELECT permission_id FROM public.permission
    WHERE name IN ('PLAN_CREATE', 'PLAN_UPDATE', 'PLAN_DELETE', 'PLAN_READ', 'TENANT_LIST', 'QUOTA_ADMIN')
);
