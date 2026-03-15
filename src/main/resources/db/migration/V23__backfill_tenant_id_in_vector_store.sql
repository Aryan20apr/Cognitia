-- =============================================================================
-- V23: Backfill tenantId into vector_store metadata for tenant isolation
-- =============================================================================
-- Existing vector_store rows lack tenantId in their metadata JSON.
-- This migration joins on raw_resource via the sourceId stored in metadata
-- and injects the tenant_id so that search-time filtering works for
-- previously ingested documents.
-- =============================================================================

UPDATE public.vector_store vs
SET metadata = jsonb_set(
    vs.metadata::jsonb,
    '{tenantId}',
    to_jsonb(rr.tenant_id::text)
)::json
FROM raw_resource rr
WHERE vs.metadata->>'sourceId' = rr.res_id::text
  AND vs.metadata->>'tenantId' IS NULL;
