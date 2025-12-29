-- =============================================================================
-- V6: Vector Store - AI Embeddings for Similarity Search
-- =============================================================================
-- Table for storing document embeddings used in RAG (Retrieval Augmented Generation).
-- Uses pgvector extension for efficient similarity search with HNSW index.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Vector store (document embeddings)
-- -----------------------------------------------------------------------------
CREATE TABLE public.vector_store (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    content text,
    metadata json,
    embedding public.vector(1536)
);

ALTER TABLE public.vector_store OWNER TO cognitia;

ALTER TABLE ONLY public.vector_store
    ADD CONSTRAINT vector_store_pkey PRIMARY KEY (id);

-- HNSW indexes for fast cosine similarity search
CREATE INDEX spring_ai_vector_index ON public.vector_store USING hnsw (embedding public.vector_cosine_ops);
CREATE INDEX vector_store_embedding_idx ON public.vector_store USING hnsw (embedding public.vector_cosine_ops);

