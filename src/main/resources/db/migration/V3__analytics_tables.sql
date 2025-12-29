-- =============================================================================
-- V3: Analytics Tables - Plans, Quotas, Billing, Usage Tracking
-- =============================================================================
-- Tables for subscription plans, tenant/user quotas, billing, and usage metrics.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Plans (subscription tiers)
-- -----------------------------------------------------------------------------
CREATE TABLE public.plans (
    id uuid NOT NULL,
    active boolean,
    code character varying(255),
    description character varying(255),
    included_completion_tokens bigint,
    included_docs bigint,
    included_prompt_tokens bigint,
    included_total_tokens bigint,
    included_users bigint,
    metadata text,
    model_restrictions text,
    name character varying(255),
    overage_pricing text,
    price_per_month numeric(38,2)
);

ALTER TABLE public.plans OWNER TO cognitia;

ALTER TABLE ONLY public.plans
    ADD CONSTRAINT plans_pkey PRIMARY KEY (id);

-- -----------------------------------------------------------------------------
-- Tenant quota (organization-level limits)
-- -----------------------------------------------------------------------------
CREATE TABLE public.tenant_quota (
    id uuid NOT NULL,
    tenant_id uuid NOT NULL,
    billing_cycle_end date,
    billing_cycle_start date,
    created_at timestamp(6) with time zone,
    currency character varying(8),
    enforcement_mode character varying(255),
    last_reset_at timestamp(6) with time zone,
    max_completion_tokens bigint,
    max_prompt_tokens bigint,
    max_resources integer,
    max_total_tokens bigint,
    max_users integer,
    overage_charges numeric(19,4),
    overage_tokens bigint,
    status character varying(32) NOT NULL,
    threshold_100_notified boolean,
    threshold_80_notified boolean,
    updated_at timestamp(6) with time zone,
    used_completion_tokens bigint,
    used_prompt_tokens bigint,
    used_resources integer,
    used_total_tokens bigint,
    used_users integer,
    plan_id uuid,
    CONSTRAINT tenant_quota_enforcement_mode_check CHECK (((enforcement_mode)::text = ANY ((ARRAY['HARD'::character varying, 'SOFT'::character varying, 'HYBRID'::character varying])::text[]))),
    CONSTRAINT tenant_quota_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'SUSPENDED'::character varying, 'EXPIRED'::character varying])::text[])))
);

ALTER TABLE public.tenant_quota OWNER TO cognitia;

ALTER TABLE ONLY public.tenant_quota
    ADD CONSTRAINT tenant_quota_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.tenant_quota
    ADD CONSTRAINT fkr41mxb3ykhvr02dn5muqr3hh0 FOREIGN KEY (plan_id) REFERENCES public.plans(id);

-- -----------------------------------------------------------------------------
-- User quota (individual user limits within a tenant)
-- -----------------------------------------------------------------------------
CREATE TABLE public.user_quota (
    id uuid NOT NULL,
    tenant_id uuid NOT NULL,
    billing_cycle_end date,
    billing_cycle_start date,
    created_at timestamp(6) with time zone,
    last_activity_at timestamp(6) with time zone,
    max_completion_tokens bigint,
    max_prompt_tokens bigint,
    max_resources integer,
    max_total_tokens bigint,
    status character varying(32) NOT NULL,
    threshold_100_notified boolean,
    threshold_80_notified boolean,
    updated_at timestamp(6) with time zone,
    used_completion_tokens bigint,
    used_prompt_tokens bigint,
    used_resources integer,
    used_total_tokens bigint,
    user_id uuid NOT NULL,
    tenant_quota_id uuid,
    CONSTRAINT user_quota_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'SUSPENDED'::character varying, 'EXPIRED'::character varying])::text[])))
);

ALTER TABLE public.user_quota OWNER TO cognitia;

ALTER TABLE ONLY public.user_quota
    ADD CONSTRAINT user_quota_pkey PRIMARY KEY (id);

CREATE INDEX idx_user_quota_tenant_id ON public.user_quota USING btree (tenant_id);
CREATE INDEX idx_user_quota_tenant_quota_id ON public.user_quota USING btree (tenant_quota_id);
CREATE INDEX idx_user_quota_user_id ON public.user_quota USING btree (user_id);

ALTER TABLE ONLY public.user_quota
    ADD CONSTRAINT fkex0sqbixm0dhtyodcaiael1s FOREIGN KEY (tenant_quota_id) REFERENCES public.tenant_quota(id);

-- -----------------------------------------------------------------------------
-- Quota action logs (limit hits, overage charges)
-- -----------------------------------------------------------------------------
CREATE TABLE public.quota_action_logs (
    id uuid NOT NULL,
    tenant_id uuid NOT NULL,
    action_type character varying(255),
    details text,
    "timestamp" timestamp(6) with time zone,
    user_id uuid,
    tenant_quota_id uuid,
    CONSTRAINT quota_action_logs_action_type_check CHECK (((action_type)::text = ANY ((ARRAY['LIMIT_HIT'::character varying, 'OVERAGE_CHARGED'::character varying])::text[])))
);

ALTER TABLE public.quota_action_logs OWNER TO cognitia;

ALTER TABLE ONLY public.quota_action_logs
    ADD CONSTRAINT quota_action_logs_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.quota_action_logs
    ADD CONSTRAINT fkq96p10nmkn57qf54sw09j48ve FOREIGN KEY (tenant_quota_id) REFERENCES public.tenant_quota(id);

-- -----------------------------------------------------------------------------
-- Aggregated usage (daily/monthly rollups)
-- -----------------------------------------------------------------------------
CREATE TABLE public.aggregated_usage (
    id uuid NOT NULL,
    tenant_id uuid NOT NULL,
    estimated_cost double precision,
    period character varying(255),
    period_start date,
    total_completion_tokens bigint,
    total_prompt_tokens bigint,
    total_tokens bigint,
    user_id uuid,
    CONSTRAINT aggregated_usage_period_check CHECK (((period)::text = ANY ((ARRAY['DAY'::character varying, 'MONTH'::character varying])::text[])))
);

ALTER TABLE public.aggregated_usage OWNER TO cognitia;

ALTER TABLE ONLY public.aggregated_usage
    ADD CONSTRAINT aggregated_usage_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.aggregated_usage
    ADD CONSTRAINT ukmgkixqxr72hcp6y8vvexu4wy UNIQUE (tenant_id, period_start);

-- -----------------------------------------------------------------------------
-- Billing records (invoices)
-- -----------------------------------------------------------------------------
CREATE TABLE public.billing_records (
    invoice_id uuid NOT NULL,
    tenant_id uuid NOT NULL,
    payment_status character varying(255),
    period_end date,
    period_start date,
    total_amount numeric(38,2),
    CONSTRAINT billing_records_payment_status_check CHECK (((payment_status)::text = ANY ((ARRAY['PENDING'::character varying, 'PAID'::character varying, 'FAILED'::character varying])::text[])))
);

ALTER TABLE public.billing_records OWNER TO cognitia;

ALTER TABLE ONLY public.billing_records
    ADD CONSTRAINT billing_records_pkey PRIMARY KEY (invoice_id);

-- -----------------------------------------------------------------------------
-- Billing usage lines (invoice line items)
-- -----------------------------------------------------------------------------
CREATE TABLE public.billing_usage_lines (
    id uuid NOT NULL,
    description character varying(255),
    quantity bigint,
    total_price numeric(38,2),
    unit_price numeric(38,2),
    billing_record_id uuid
);

ALTER TABLE public.billing_usage_lines OWNER TO cognitia;

ALTER TABLE ONLY public.billing_usage_lines
    ADD CONSTRAINT billing_usage_lines_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.billing_usage_lines
    ADD CONSTRAINT fkesv9y6j3fvg7i1lxdt82caxwj FOREIGN KEY (billing_record_id) REFERENCES public.billing_records(invoice_id);

