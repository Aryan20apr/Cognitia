-- =============================================================================
-- V8: Add plan_id to tenants table
-- =============================================================================
-- Links tenants to their subscription plans
-- =============================================================================




ALTER TABLE public.tenants ADD COLUMN plan_id uuid;

ALTER TABLE public.tenants ADD CONSTRAINT fk_tenant_plan FOREIGN KEY (plan_id) REFERENCES public.plans(id);

COMMENT ON COLUMN public.tenants.plan_id IS 'Reference to the subscription plan assigned to this tenant';


UPDATE public.tenants 
SET plan_id = (
    SELECT id FROM public.plans 
    WHERE code = 'TRIAL001' 
    LIMIT 1
)
WHERE plan_id IS NULL;