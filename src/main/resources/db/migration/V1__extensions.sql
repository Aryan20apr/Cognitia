-- =============================================================================
-- V1: PostgreSQL Extensions
-- =============================================================================
-- Install required PostgreSQL extensions for the Cognitia application.
-- These provide UUID generation, key-value storage, and vector similarity search.
-- =============================================================================

-- hstore: Key-value storage for flexible metadata
CREATE EXTENSION IF NOT EXISTS hstore WITH SCHEMA public;
COMMENT ON EXTENSION hstore IS 'data type for storing sets of (key, value) pairs';

-- uuid-ossp: UUID generation functions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA public;
COMMENT ON EXTENSION "uuid-ossp" IS 'generate universally unique identifiers (UUIDs)';

-- pgvector: Vector embeddings for AI/ML similarity search
CREATE EXTENSION IF NOT EXISTS vector WITH SCHEMA public;
COMMENT ON EXTENSION vector IS 'vector data type and ivfflat and hnsw access methods';

