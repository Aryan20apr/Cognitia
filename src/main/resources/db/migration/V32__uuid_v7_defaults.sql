-- PL/pgSQL function that generates RFC 9562-compliant UUID v7.
-- The top 48 bits are the current Unix timestamp in milliseconds,
-- bits 49-52 are the version (0111 = 7), bits 63-64 are the variant (10),
-- and the remaining bits are random.
CREATE OR REPLACE FUNCTION uuid_generate_v7()
RETURNS UUID
LANGUAGE plpgsql
AS $$
DECLARE
    unix_ts_ms BIGINT;
    rand_bytes BYTEA;
    hi         BIGINT;
    lo         BIGINT;
BEGIN
    unix_ts_ms := (EXTRACT(EPOCH FROM clock_timestamp()) * 1000)::BIGINT;

    rand_bytes := gen_random_bytes(10);

    -- High 64 bits: 48-bit timestamp | 4-bit version (7) | 12 random bits
    hi := (unix_ts_ms << 16)
        | (7::BIGINT << 12)
        | ((get_byte(rand_bytes, 0)::BIGINT << 4) | (get_byte(rand_bytes, 1)::BIGINT >> 4) & 15);

    -- Low 64 bits: 2-bit variant (10) | 62 random bits
    lo := ((2::BIGINT << 62)
        | ((get_byte(rand_bytes, 1)::BIGINT & 15) << 58)
        | (get_byte(rand_bytes, 2)::BIGINT << 50)
        | (get_byte(rand_bytes, 3)::BIGINT << 42)
        | (get_byte(rand_bytes, 4)::BIGINT << 34)
        | (get_byte(rand_bytes, 5)::BIGINT << 26)
        | (get_byte(rand_bytes, 6)::BIGINT << 18)
        | (get_byte(rand_bytes, 7)::BIGINT << 10)
        | (get_byte(rand_bytes, 8)::BIGINT << 2)
        | (get_byte(rand_bytes, 9)::BIGINT >> 6));

    RETURN encode(
        set_byte(set_byte(set_byte(set_byte(set_byte(set_byte(set_byte(set_byte(
            decode(lpad(to_hex(hi), 16, '0') || lpad(to_hex(lo), 16, '0'), 'hex'),
        6, (get_byte(decode(lpad(to_hex(hi), 16, '0'), 'hex'), 6) & 15) | 112),
        8, (get_byte(decode(lpad(to_hex(lo), 16, '0'), 'hex'), 0) & 63) | 128),
        0, (hi >> 56)::INT & 255),
        1, (hi >> 48)::INT & 255),
        2, (hi >> 40)::INT & 255),
        3, (hi >> 32)::INT & 255),
        4, (hi >> 24)::INT & 255),
        5, (hi >> 16)::INT & 255)
    , 'hex')::UUID;
END;
$$;

-- Switch DEFAULT on UUID PK columns that may receive raw SQL inserts.
-- JPA-managed inserts are unaffected (Hibernate generates the ID in Java).
ALTER TABLE departments           ALTER COLUMN id SET DEFAULT uuid_generate_v7();
ALTER TABLE classification_levels ALTER COLUMN id SET DEFAULT uuid_generate_v7();
ALTER TABLE tenants               ALTER COLUMN id SET DEFAULT uuid_generate_v7();
ALTER TABLE users                 ALTER COLUMN id SET DEFAULT uuid_generate_v7();
ALTER TABLE raw_resource          ALTER COLUMN res_id SET DEFAULT uuid_generate_v7();
ALTER TABLE ingestion_job         ALTER COLUMN id SET DEFAULT uuid_generate_v7();
ALTER TABLE chat_threads          ALTER COLUMN id SET DEFAULT uuid_generate_v7();
ALTER TABLE chat_messages         ALTER COLUMN id SET DEFAULT uuid_generate_v7();
ALTER TABLE orders                ALTER COLUMN id SET DEFAULT uuid_generate_v7();
ALTER TABLE payments              ALTER COLUMN id SET DEFAULT uuid_generate_v7();
ALTER TABLE refresh_token         ALTER COLUMN id SET DEFAULT uuid_generate_v7();
