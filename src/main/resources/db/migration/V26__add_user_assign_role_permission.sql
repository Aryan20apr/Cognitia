INSERT INTO public.permission (name, created_at, updated_at)
SELECT 'USER_ASSIGN_ROLE', NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM public.permission WHERE name = 'USER_ASSIGN_ROLE'
);

INSERT INTO public.role_permisson (role_id, permission_id)
SELECT r.role_id,
       (SELECT permission_id FROM public.permission WHERE name = 'USER_ASSIGN_ROLE')
FROM public.roles r
WHERE r.role_name = 'SUPER_ADMIN'
ON CONFLICT (role_id, permission_id) DO NOTHING;
