-- =============================================================================
-- V5: Ingestion Tables - Resources and Ingestion Jobs
-- =============================================================================
-- Tables for document upload, storage, and processing pipeline.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Raw resources (uploaded files/documents)
-- -----------------------------------------------------------------------------
CREATE TABLE public.raw_resource (
    res_id uuid NOT NULL,
    asset_id character varying(255) NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    description character varying(255) NOT NULL,
    format character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    secure_url character varying(255) NOT NULL,
    signature character varying(255) NOT NULL,
    size double precision NOT NULL,
    status character varying(255) NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    url character varying(255) NOT NULL,
    tenant_id uuid NOT NULL,
    CONSTRAINT raw_resource_status_check CHECK (((status)::text = ANY ((ARRAY['UPLOADING'::character varying, 'UPLOADED'::character varying])::text[])))
);

ALTER TABLE public.raw_resource OWNER TO cognitia;

ALTER TABLE ONLY public.raw_resource
    ADD CONSTRAINT raw_resource_pkey PRIMARY KEY (res_id);

-- Unique constraints for deduplication
ALTER TABLE ONLY public.raw_resource
    ADD CONSTRAINT uk6in9gypl2mxbsxf8pt0cqht1a UNIQUE (name);

ALTER TABLE ONLY public.raw_resource
    ADD CONSTRAINT uk762yga9iu3djsmmyhk5y8euic UNIQUE (asset_id);

ALTER TABLE ONLY public.raw_resource
    ADD CONSTRAINT uk8yst6yv454rqc2otrffq0sby8 UNIQUE (url);

ALTER TABLE ONLY public.raw_resource
    ADD CONSTRAINT ukbpobdyryosyg5hrl4tksi8q4s UNIQUE (secure_url);

ALTER TABLE ONLY public.raw_resource
    ADD CONSTRAINT ukotsu0nu0a4xw7ondebn4s472e UNIQUE (signature);

-- Indexes for fast lookups
CREATE INDEX idx_resource_id ON public.raw_resource USING btree (res_id);
CREATE INDEX idx_resource_name ON public.raw_resource USING btree (name);

-- -----------------------------------------------------------------------------
-- Ingestion jobs (document processing pipeline)
-- -----------------------------------------------------------------------------
CREATE TABLE public.ingestion_job (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    retries integer NOT NULL,
    status smallint NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    res_id uuid,
    CONSTRAINT ingestion_job_status_check CHECK (((status >= 0) AND (status <= 2)))
);

ALTER TABLE public.ingestion_job OWNER TO cognitia;

ALTER TABLE ONLY public.ingestion_job
    ADD CONSTRAINT ingestion_job_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.ingestion_job
    ADD CONSTRAINT uk3ug5qhyvx4hu8xume5321qrjb UNIQUE (res_id);

ALTER TABLE ONLY public.ingestion_job
    ADD CONSTRAINT fkcq1at9d04rdj1e7k7kmc22glf FOREIGN KEY (res_id) REFERENCES public.raw_resource(res_id);

