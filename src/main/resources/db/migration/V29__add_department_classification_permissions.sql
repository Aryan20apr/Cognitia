INSERT INTO public.permission (name, created_at, updated_at)
SELECT 'DEPARTMENT_MANAGE', NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM public.permission WHERE name = 'DEPARTMENT_MANAGE'
);

INSERT INTO public.permission (name, created_at, updated_at)
SELECT 'CLASSIFICATION_MANAGE', NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM public.permission WHERE name = 'CLASSIFICATION_MANAGE'
);

INSERT INTO public.role_permisson (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM public.roles r
CROSS JOIN public.permission p
WHERE r.role_name = 'SUPER_ADMIN'
  AND p.name IN ('DEPARTMENT_MANAGE', 'CLASSIFICATION_MANAGE')
ON CONFLICT (role_id, permission_id) DO NOTHING;
