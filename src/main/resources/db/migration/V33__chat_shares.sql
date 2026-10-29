-- =============================================================================
-- V33: Chat Sharing Tables
-- =============================================================================
-- Tables for sharing chat threads with frozen snapshots.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Chat shares (the share itself with metadata)
-- -----------------------------------------------------------------------------
CREATE TABLE public.chat_shares (
    id uuid NOT NULL,
    tenant_id uuid NOT NULL,
    source_thread_id uuid NOT NULL,
    owner_user_id uuid NOT NULL,
    token character varying(22) NOT NULL,
    visibility character varying(20) NOT NULL,
    title character varying(255),
    snapshot_version integer NOT NULL DEFAULT 1,
    expires_at timestamp(6) with time zone,
    revoked_at timestamp(6) with time zone,
    revoked_by uuid,
    created_at timestamp(6) with time zone NOT NULL DEFAULT now(),
    CONSTRAINT chat_shares_pkey PRIMARY KEY (id),
    CONSTRAINT chat_shares_token_unique UNIQUE (token),
    CONSTRAINT chat_shares_visibility_check CHECK (visibility IN ('NAMED', 'ROLE_BASED', 'TENANT_WIDE', 'REVOKED'))
);

ALTER TABLE public.chat_shares OWNER TO cognitia;

ALTER TABLE ONLY public.chat_shares
    ADD CONSTRAINT fk_chat_shares_source_thread FOREIGN KEY (source_thread_id) REFERENCES public.chat_threads(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.chat_shares
    ADD CONSTRAINT fk_chat_shares_owner_user FOREIGN KEY (owner_user_id) REFERENCES public.users(id);

ALTER TABLE ONLY public.chat_shares
    ADD CONSTRAINT fk_chat_shares_revoked_by FOREIGN KEY (revoked_by) REFERENCES public.users(id);

CREATE INDEX idx_chat_shares_source_thread ON public.chat_shares USING btree (source_thread_id);
CREATE INDEX idx_chat_shares_tenant ON public.chat_shares USING btree (tenant_id);
CREATE INDEX idx_chat_shares_owner ON public.chat_shares USING btree (owner_user_id);

-- -----------------------------------------------------------------------------
-- Chat share messages (frozen snapshot of messages at share time)
-- -----------------------------------------------------------------------------
CREATE TABLE public.chat_share_messages (
    id uuid NOT NULL,
    share_id uuid NOT NULL,
    snapshot_version integer NOT NULL,
    sequence_no integer NOT NULL,
    sender smallint NOT NULL,
    content text NOT NULL,
    original_message_id uuid,
    original_created_at timestamp(6) with time zone NOT NULL,
    CONSTRAINT chat_share_messages_pkey PRIMARY KEY (id),
    CONSTRAINT chat_share_messages_unique UNIQUE (share_id, snapshot_version, sequence_no),
    CONSTRAINT chat_share_messages_sender_check CHECK (((sender >= 0) AND (sender <= 3)))
);

ALTER TABLE public.chat_share_messages OWNER TO cognitia;

ALTER TABLE ONLY public.chat_share_messages
    ADD CONSTRAINT fk_chat_share_messages_share FOREIGN KEY (share_id) REFERENCES public.chat_shares(id) ON DELETE CASCADE;

CREATE INDEX idx_chat_share_messages_share ON public.chat_share_messages USING btree (share_id, snapshot_version, sequence_no);

-- -----------------------------------------------------------------------------
-- Chat share grants (explicit access grants for NAMED and ROLE_BASED shares)
-- -----------------------------------------------------------------------------
CREATE TABLE public.chat_share_grants (
    id uuid NOT NULL,
    share_id uuid NOT NULL,
    grantee_user_id uuid,
    grantee_role_id integer,
    granted_by uuid NOT NULL,
    granted_at timestamp(6) with time zone NOT NULL DEFAULT now(),
    CONSTRAINT chat_share_grants_pkey PRIMARY KEY (id),
    CONSTRAINT chat_share_grants_user_unique UNIQUE (share_id, grantee_user_id),
    CONSTRAINT chat_share_grants_role_unique UNIQUE (share_id, grantee_role_id),
    CONSTRAINT chat_share_grants_grantee_check CHECK (
        (grantee_user_id IS NOT NULL AND grantee_role_id IS NULL) OR
        (grantee_user_id IS NULL AND grantee_role_id IS NOT NULL)
    )
);

ALTER TABLE public.chat_share_grants OWNER TO cognitia;

ALTER TABLE ONLY public.chat_share_grants
    ADD CONSTRAINT fk_chat_share_grants_share FOREIGN KEY (share_id) REFERENCES public.chat_shares(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.chat_share_grants
    ADD CONSTRAINT fk_chat_share_grants_grantee_user FOREIGN KEY (grantee_user_id) REFERENCES public.users(id);

ALTER TABLE ONLY public.chat_share_grants
    ADD CONSTRAINT fk_chat_share_grants_grantee_role FOREIGN KEY (grantee_role_id) REFERENCES public.roles(role_id);

ALTER TABLE ONLY public.chat_share_grants
    ADD CONSTRAINT fk_chat_share_grants_granted_by FOREIGN KEY (granted_by) REFERENCES public.users(id);

CREATE INDEX idx_chat_share_grants_user ON public.chat_share_grants USING btree (grantee_user_id);
CREATE INDEX idx_chat_share_grants_role ON public.chat_share_grants USING btree (grantee_role_id);
CREATE INDEX idx_chat_share_grants_share ON public.chat_share_grants USING btree (share_id);
