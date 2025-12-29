-- =============================================================================
-- V4: Chat Tables - Threads, Messages, Usage Events
-- =============================================================================
-- Tables for AI chat conversations and token usage tracking.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Chat threads (conversations)
-- -----------------------------------------------------------------------------
CREATE TABLE public.chat_threads (
    id uuid NOT NULL,
    tenant_id uuid NOT NULL,
    created_at timestamp(6) with time zone,
    title character varying(255),
    user_id uuid
);

ALTER TABLE public.chat_threads OWNER TO cognitia;

ALTER TABLE ONLY public.chat_threads
    ADD CONSTRAINT chat_threads_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.chat_threads
    ADD CONSTRAINT fk53itvr5ltymlahvy7ywt9dr7e FOREIGN KEY (user_id) REFERENCES public.users(id);

-- -----------------------------------------------------------------------------
-- Chat messages (individual messages in a thread)
-- -----------------------------------------------------------------------------
CREATE TABLE public.chat_messages (
    id uuid NOT NULL,
    content text,
    created_at timestamp(6) without time zone NOT NULL,
    sender smallint,
    thread_id uuid,
    CONSTRAINT chat_messages_sender_check CHECK (((sender >= 0) AND (sender <= 3)))
);

ALTER TABLE public.chat_messages OWNER TO cognitia;

ALTER TABLE ONLY public.chat_messages
    ADD CONSTRAINT chat_messages_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.chat_messages
    ADD CONSTRAINT fkm6wwwxkgq8x9iwp58j8n2ujy9 FOREIGN KEY (thread_id) REFERENCES public.chat_threads(id);

-- -----------------------------------------------------------------------------
-- Chat usage events (token consumption per request)
-- -----------------------------------------------------------------------------
CREATE TABLE public.chat_usage_event (
    id uuid NOT NULL,
    tenant_id uuid NOT NULL,
    completion_tokens bigint,
    created_at timestamp(6) without time zone,
    estimated_cost double precision,
    is_processed boolean,
    latency_ms bigint,
    meta_data_json text,
    model_name character varying(255),
    processed_at timestamp(6) without time zone,
    prompt_tokens bigint,
    request_id character varying(255) NOT NULL,
    total_tokens bigint,
    updated_at timestamp(6) without time zone,
    thread_id uuid,
    user_id uuid
);

ALTER TABLE public.chat_usage_event OWNER TO cognitia;

ALTER TABLE ONLY public.chat_usage_event
    ADD CONSTRAINT chat_usage_event_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.chat_usage_event
    ADD CONSTRAINT uk13x5uut62cw5qymsyln0vl3su UNIQUE (request_id);

CREATE INDEX idx_cue_request ON public.chat_usage_event USING btree (request_id);
CREATE INDEX idx_cue_tenant ON public.chat_usage_event USING btree (tenant_id);
CREATE INDEX idx_cue_user ON public.chat_usage_event USING btree (user_id);

ALTER TABLE ONLY public.chat_usage_event
    ADD CONSTRAINT fk81j6rjwlfclma9kvnh48x8cjg FOREIGN KEY (user_id) REFERENCES public.users(id);

ALTER TABLE ONLY public.chat_usage_event
    ADD CONSTRAINT fkfxrmaib9d0kqlflham64k9vcf FOREIGN KEY (thread_id) REFERENCES public.chat_threads(id);

