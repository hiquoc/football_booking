CREATE INDEX IF NOT EXISTS idx_fields_public_legacy_location
    ON fields (legacy_province, legacy_district)
    WHERE status = 'APPROVED' AND active = true AND deleted = false;
