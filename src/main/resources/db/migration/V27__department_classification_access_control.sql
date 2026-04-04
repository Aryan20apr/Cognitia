-- Departments: tenant-scoped organizational units
CREATE TABLE IF NOT EXISTS departments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now(),
    UNIQUE (tenant_id, name)
);

CREATE INDEX idx_departments_tenant ON departments(tenant_id);

-- Classification levels: tenant-scoped sensitivity tiers with integer rank
CREATE TABLE IF NOT EXISTS classification_levels (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    rank INTEGER NOT NULL,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now(),
    UNIQUE (tenant_id, name),
    UNIQUE (tenant_id, rank)
);

CREATE INDEX idx_classification_levels_tenant ON classification_levels(tenant_id);

-- Join table: users can belong to multiple departments
CREATE TABLE IF NOT EXISTS user_departments (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    department_id UUID NOT NULL REFERENCES departments(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, department_id)
);

-- Add clearance_rank to roles
ALTER TABLE roles ADD COLUMN IF NOT EXISTS clearance_rank INTEGER DEFAULT 0;

-- Add department and classification references to raw_resource
ALTER TABLE raw_resource ADD COLUMN IF NOT EXISTS department_id UUID REFERENCES departments(id) ON DELETE SET NULL;
ALTER TABLE raw_resource ADD COLUMN IF NOT EXISTS classification_level_id UUID REFERENCES classification_levels(id) ON DELETE SET NULL;

-- Seed default department and classification levels for all existing tenants
INSERT INTO departments (id, name, tenant_id)
SELECT gen_random_uuid(), 'General', t.id
FROM tenants t
WHERE NOT EXISTS (
    SELECT 1 FROM departments d WHERE d.tenant_id = t.id AND d.name = 'General'
);

INSERT INTO classification_levels (id, name, rank, tenant_id)
SELECT gen_random_uuid(), level.name, level.rank, t.id
FROM tenants t
CROSS JOIN (VALUES ('Public', 0), ('Internal', 10), ('Confidential', 20), ('Restricted', 30)) AS level(name, rank)
WHERE NOT EXISTS (
    SELECT 1 FROM classification_levels cl WHERE cl.tenant_id = t.id AND cl.name = level.name
);

-- Set SUPER_ADMIN roles to max clearance
UPDATE roles SET clearance_rank = 30 WHERE role_name = 'SUPER_ADMIN' AND clearance_rank IS NULL OR clearance_rank = 0;
