ALTER TABLE roles ADD COLUMN IF NOT EXISTS clearance_level_id UUID REFERENCES classification_levels(id) ON DELETE SET NULL;

UPDATE roles r
SET clearance_level_id = (
    SELECT cl.id
    FROM classification_levels cl
    WHERE cl.tenant_id = r.tenant_id
      AND cl.rank = r.clearance_rank
    LIMIT 1
)
WHERE r.clearance_rank > 0
  AND r.clearance_level_id IS NULL;

ALTER TABLE roles DROP COLUMN IF EXISTS clearance_rank;
