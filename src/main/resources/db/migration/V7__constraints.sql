-- =============================================================================
-- V7: Additional Constraints and Refinements
-- =============================================================================
-- Add missing NOT NULL constraints, unique constraints, and indexes based on
-- entity definitions to ensure data integrity.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Tenants table constraints
-- -----------------------------------------------------------------------------
-- Ensure tenant name is not null (used in hashCode/equals)
ALTER TABLE public.tenants
    ALTER COLUMN name SET NOT NULL;

-- -----------------------------------------------------------------------------
-- Users table constraints
-- -----------------------------------------------------------------------------
-- User.name is not marked nullable=false in entity but used in equals/hashCode
-- Keep it nullable for now but ensure other fields are NOT NULL
-- email, phone_number, password already have NOT NULL in V2

-- -----------------------------------------------------------------------------
-- Roles table constraints
-- -----------------------------------------------------------------------------
-- role_name is marked nullable=false in entity
-- Already set in V2

-- -----------------------------------------------------------------------------
-- Plans table constraints
-- -----------------------------------------------------------------------------
-- Add unique constraint on plan code for easier lookups
ALTER TABLE public.plans
    ADD CONSTRAINT uk_plan_code UNIQUE (code);

-- -----------------------------------------------------------------------------
-- Tenant Quota table constraints
-- -----------------------------------------------------------------------------
-- Ensure tenant_quota has unique constraint per tenant (one active quota per tenant)
-- Note: This allows multiple quotas per tenant over time, but consider adding
-- a unique partial index if only one active quota per tenant is desired:
-- CREATE UNIQUE INDEX uk_tenant_quota_active ON public.tenant_quota (tenant_id) 
--   WHERE status = 'ACTIVE';

-- -----------------------------------------------------------------------------
-- User Quota table constraints
-- -----------------------------------------------------------------------------
-- Ensure one quota per user per tenant (user_id is already indexed in V3)
-- Add unique constraint on user_id to prevent duplicate user quotas
ALTER TABLE public.user_quota
    ADD CONSTRAINT uk_user_quota_user_id UNIQUE (user_id);

-- -----------------------------------------------------------------------------
-- Chat Threads table constraints
-- -----------------------------------------------------------------------------
-- Ensure created_at is not null (entity has default value)
ALTER TABLE public.chat_threads
    ALTER COLUMN created_at SET NOT NULL;

-- -----------------------------------------------------------------------------
-- Chat Messages table constraints
-- -----------------------------------------------------------------------------
-- created_at already has NOT NULL in V4

-- -----------------------------------------------------------------------------
-- Raw Resource table constraints
-- -----------------------------------------------------------------------------
-- tenant_id should be NOT NULL (extends TenantAwareEntity)
ALTER TABLE public.raw_resource
    ALTER COLUMN tenant_id SET NOT NULL;

-- -----------------------------------------------------------------------------
-- Ingestion Job table constraints
-- -----------------------------------------------------------------------------
-- Add unique constraint on res_id (already exists in V5 as uk3ug5qhyvx4hu8xume5321qrjb)
-- Ensure retries has default value
ALTER TABLE public.ingestion_job
    ALTER COLUMN retries SET DEFAULT 0;

-- -----------------------------------------------------------------------------
-- Chat Usage Event table constraints
-- -----------------------------------------------------------------------------
-- Ensure tenant_id is NOT NULL (extends TenantAwareEntity)
-- request_id already has unique + not null in V4

-- -----------------------------------------------------------------------------
-- Aggregated Usage table constraints
-- -----------------------------------------------------------------------------
-- Ensure period is not null
ALTER TABLE public.aggregated_usage
    ALTER COLUMN period SET NOT NULL;

ALTER TABLE public.aggregated_usage
    ALTER COLUMN period_start SET NOT NULL;

-- -----------------------------------------------------------------------------
-- Billing Records table constraints
-- -----------------------------------------------------------------------------
-- Ensure tenant_id is NOT NULL
ALTER TABLE public.billing_records
    ALTER COLUMN tenant_id SET NOT NULL;

-- -----------------------------------------------------------------------------
-- Quota Action Logs table constraints
-- -----------------------------------------------------------------------------
-- Ensure timestamp is not null
ALTER TABLE public.quota_action_logs
    ALTER COLUMN "timestamp" SET NOT NULL;

-- -----------------------------------------------------------------------------
-- Permissions table constraints
-- -----------------------------------------------------------------------------
-- Add unique constraint on permission name to prevent duplicates
ALTER TABLE public.permission
    ADD CONSTRAINT uk_permission_name UNIQUE (name);

-- -----------------------------------------------------------------------------
-- Additional indexes for performance
-- -----------------------------------------------------------------------------
-- Index on chat_threads.tenant_id for tenant filtering
CREATE INDEX IF NOT EXISTS idx_chat_threads_tenant_id ON public.chat_threads (tenant_id);

-- Index on chat_threads.user_id for user's thread lookups
CREATE INDEX IF NOT EXISTS idx_chat_threads_user_id ON public.chat_threads (user_id);

-- Index on chat_messages.created_at for chronological queries
CREATE INDEX IF NOT EXISTS idx_chat_messages_created_at ON public.chat_messages (created_at);

-- Index on chat_usage_event.created_at for time-based queries
CREATE INDEX IF NOT EXISTS idx_chat_usage_event_created_at ON public.chat_usage_event (created_at);

-- Index on chat_usage_event.is_processed for filtering unprocessed events
CREATE INDEX IF NOT EXISTS idx_chat_usage_event_is_processed ON public.chat_usage_event (is_processed) WHERE is_processed = false;

-- Index on tenant_quota.tenant_id for tenant lookups
CREATE INDEX IF NOT EXISTS idx_tenant_quota_tenant_id ON public.tenant_quota (tenant_id);

-- Index on tenant_quota.status for filtering active quotas
CREATE INDEX IF NOT EXISTS idx_tenant_quota_status ON public.tenant_quota (status);

-- Index on raw_resource.tenant_id for tenant filtering
CREATE INDEX IF NOT EXISTS idx_raw_resource_tenant_id ON public.raw_resource (tenant_id);

-- Index on raw_resource.status for filtering by upload status
CREATE INDEX IF NOT EXISTS idx_raw_resource_status ON public.raw_resource (status);

-- Index on ingestion_job.status for filtering by processing status
CREATE INDEX IF NOT EXISTS idx_ingestion_job_status ON public.ingestion_job (status);

-- Index on aggregated_usage.tenant_id and period_start for efficient rollup queries
CREATE INDEX IF NOT EXISTS idx_aggregated_usage_tenant_period ON public.aggregated_usage (tenant_id, period_start);

-- Index on billing_records.tenant_id and period_start for invoice lookups
CREATE INDEX IF NOT EXISTS idx_billing_records_tenant_period ON public.billing_records (tenant_id, period_start);

-- Index on roles.tenant_id for tenant-scoped role lookups
CREATE INDEX IF NOT EXISTS idx_roles_tenant_id ON public.roles (tenant_id);

-- Index on users.tenant_id for tenant filtering (if not already exists)
CREATE INDEX IF NOT EXISTS idx_users_tenant_id ON public.users (tenant_id);

-- Index on refresh_token.user_id for user token lookups (if not already exists)
CREATE INDEX IF NOT EXISTS idx_refresh_token_user_id ON public.refresh_token (user_id);

-- Index on refresh_token.token_hash for token validation
CREATE INDEX IF NOT EXISTS idx_refresh_token_token_hash ON public.refresh_token (token_hash);

-- Index on refresh_token.expires_at for cleanup of expired tokens
CREATE INDEX IF NOT EXISTS idx_refresh_token_expires_at ON public.refresh_token (expires_at);

-- -----------------------------------------------------------------------------
-- Check constraints for data validation
-- -----------------------------------------------------------------------------
-- Ensure positive values for token counts in tenant_quota
ALTER TABLE public.tenant_quota
    ADD CONSTRAINT chk_tenant_quota_positive_tokens CHECK (
        (max_prompt_tokens IS NULL OR max_prompt_tokens >= 0) AND
        (max_completion_tokens IS NULL OR max_completion_tokens >= 0) AND
        (max_total_tokens IS NULL OR max_total_tokens >= 0) AND
        (used_prompt_tokens IS NULL OR used_prompt_tokens >= 0) AND
        (used_completion_tokens IS NULL OR used_completion_tokens >= 0) AND
        (used_total_tokens IS NULL OR used_total_tokens >= 0)
    );

-- Ensure positive values for resource counts in tenant_quota
ALTER TABLE public.tenant_quota
    ADD CONSTRAINT chk_tenant_quota_positive_resources CHECK (
        (max_resources IS NULL OR max_resources >= 0) AND
        (max_users IS NULL OR max_users >= 0) AND
        (used_resources IS NULL OR used_resources >= 0) AND
        (used_users IS NULL OR used_users >= 0)
    );

-- Ensure positive values for token counts in user_quota
ALTER TABLE public.user_quota
    ADD CONSTRAINT chk_user_quota_positive_tokens CHECK (
        (max_prompt_tokens IS NULL OR max_prompt_tokens >= 0) AND
        (max_completion_tokens IS NULL OR max_completion_tokens >= 0) AND
        (max_total_tokens IS NULL OR max_total_tokens >= 0) AND
        (used_prompt_tokens IS NULL OR used_prompt_tokens >= 0) AND
        (used_completion_tokens IS NULL OR used_completion_tokens >= 0) AND
        (used_total_tokens IS NULL OR used_total_tokens >= 0)
    );

-- Ensure positive values for resource counts in user_quota
ALTER TABLE public.user_quota
    ADD CONSTRAINT chk_user_quota_positive_resources CHECK (
        (max_resources IS NULL OR max_resources >= 0) AND
        (used_resources IS NULL OR used_resources >= 0)
    );

-- Ensure positive token counts in chat_usage_event
ALTER TABLE public.chat_usage_event
    ADD CONSTRAINT chk_chat_usage_positive_tokens CHECK (
        (prompt_tokens IS NULL OR prompt_tokens >= 0) AND
        (completion_tokens IS NULL OR completion_tokens >= 0) AND
        (total_tokens IS NULL OR total_tokens >= 0)
    );

-- Ensure positive latency
ALTER TABLE public.chat_usage_event
    ADD CONSTRAINT chk_chat_usage_positive_latency CHECK (
        latency_ms IS NULL OR latency_ms >= 0
    );

-- Ensure positive token counts in aggregated_usage
ALTER TABLE public.aggregated_usage
    ADD CONSTRAINT chk_aggregated_usage_positive_tokens CHECK (
        (total_prompt_tokens IS NULL OR total_prompt_tokens >= 0) AND
        (total_completion_tokens IS NULL OR total_completion_tokens >= 0) AND
        (total_tokens IS NULL OR total_tokens >= 0)
    );

-- Ensure positive resource size
ALTER TABLE public.raw_resource
    ADD CONSTRAINT chk_raw_resource_positive_size CHECK (
        size >= 0
    );

-- Ensure billing cycle dates are valid
ALTER TABLE public.tenant_quota
    ADD CONSTRAINT chk_tenant_quota_billing_cycle CHECK (
        billing_cycle_start IS NULL OR billing_cycle_end IS NULL OR 
        billing_cycle_end >= billing_cycle_start
    );

ALTER TABLE public.user_quota
    ADD CONSTRAINT chk_user_quota_billing_cycle CHECK (
        billing_cycle_start IS NULL OR billing_cycle_end IS NULL OR 
        billing_cycle_end >= billing_cycle_start
    );

ALTER TABLE public.billing_records
    ADD CONSTRAINT chk_billing_records_period CHECK (
        period_start IS NULL OR period_end IS NULL OR 
        period_end >= period_start
    );

-- Ensure positive billing amounts
ALTER TABLE public.billing_records
    ADD CONSTRAINT chk_billing_records_positive_amount CHECK (
        total_amount IS NULL OR total_amount >= 0
    );

ALTER TABLE public.billing_usage_lines
    ADD CONSTRAINT chk_billing_usage_lines_positive CHECK (
        (quantity IS NULL OR quantity >= 0) AND
        (unit_price IS NULL OR unit_price >= 0) AND
        (total_price IS NULL OR total_price >= 0)
    );

-- Ensure positive plan pricing
ALTER TABLE public.plans
    ADD CONSTRAINT chk_plans_positive_price CHECK (
        price_per_month IS NULL OR price_per_month >= 0
    );

-- Ensure positive included limits in plans
ALTER TABLE public.plans
    ADD CONSTRAINT chk_plans_positive_limits CHECK (
        (included_prompt_tokens IS NULL OR included_prompt_tokens >= 0) AND
        (included_completion_tokens IS NULL OR included_completion_tokens >= 0) AND
        (included_total_tokens IS NULL OR included_total_tokens >= 0) AND
        (included_docs IS NULL OR included_docs >= 0) AND
        (included_users IS NULL OR included_users >= 0)
    );

