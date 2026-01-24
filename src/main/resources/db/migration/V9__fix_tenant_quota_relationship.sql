-- =============================================================================
-- V9: Fix Tenant-TenantQuota Relationship
-- =============================================================================
-- Removes direct Tenant->Plan mapping
-- =============================================================================


ALTER TABLE public.tenants DROP CONSTRAINT IF EXISTS fk_tenant_plan;
DROP INDEX IF EXISTS idx_tenants_plan_id;
ALTER TABLE public.tenants DROP COLUMN IF EXISTS plan_id;