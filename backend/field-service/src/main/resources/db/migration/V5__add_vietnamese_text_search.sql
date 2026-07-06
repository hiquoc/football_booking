CREATE EXTENSION IF NOT EXISTS unaccent;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- PostgreSQL marks unaccent as STABLE. This immutable wrapper allows the same
-- normalization expression to be indexed and used safely by search queries.
CREATE OR REPLACE FUNCTION vietnamese_search_normalize(input TEXT)
RETURNS TEXT
LANGUAGE SQL
IMMUTABLE
STRICT
PARALLEL SAFE
AS $$
    SELECT lower(public.unaccent('public.unaccent'::regdictionary, trim(input)))
$$;

DROP INDEX IF EXISTS idx_fields_public_legacy_location;

CREATE INDEX IF NOT EXISTS idx_fields_public_legacy_location_normalized
    ON fields (
        vietnamese_search_normalize(legacy_province),
        vietnamese_search_normalize(legacy_district)
    )
    WHERE status = 'APPROVED' AND active = true AND deleted = false;

CREATE INDEX IF NOT EXISTS idx_fields_public_name_vietnamese_search
    ON fields USING GIN (vietnamese_search_normalize(name) gin_trgm_ops)
    WHERE status = 'APPROVED' AND active = true AND deleted = false;

CREATE INDEX IF NOT EXISTS idx_fields_public_address_vietnamese_search
    ON fields USING GIN (vietnamese_search_normalize(address) gin_trgm_ops)
    WHERE status = 'APPROVED' AND active = true AND deleted = false;
