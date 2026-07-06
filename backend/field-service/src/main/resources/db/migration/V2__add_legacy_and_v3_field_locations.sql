ALTER TABLE fields
    ADD COLUMN IF NOT EXISTS ward VARCHAR(255),
    ADD COLUMN IF NOT EXISTS ward_code VARCHAR(20),
    ADD COLUMN IF NOT EXISTS province VARCHAR(255),
    ADD COLUMN IF NOT EXISTS province_code VARCHAR(20),
    ADD COLUMN IF NOT EXISTS legacy_ward VARCHAR(255),
    ADD COLUMN IF NOT EXISTS legacy_ward_code VARCHAR(20),
    ADD COLUMN IF NOT EXISTS legacy_district VARCHAR(255),
    ADD COLUMN IF NOT EXISTS legacy_province VARCHAR(255);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'fields'
          AND column_name = 'district'
    ) THEN
        EXECUTE 'UPDATE fields SET legacy_district = COALESCE(legacy_district, district)';
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'fields'
          AND column_name = 'city'
    ) THEN
        EXECUTE 'UPDATE fields SET legacy_province = COALESCE(legacy_province, city)';
    END IF;
END $$;

UPDATE fields
SET legacy_ward = COALESCE(legacy_ward, ward, 'Chưa cập nhật'),
    legacy_ward_code = COALESCE(legacy_ward_code, ward_code, 'UNKNOWN'),
    legacy_district = COALESCE(legacy_district, 'Chưa cập nhật'),
    legacy_province = COALESCE(legacy_province, province, 'Chưa cập nhật'),
    ward = COALESCE(ward, legacy_ward, 'Chưa cập nhật'),
    ward_code = COALESCE(ward_code, legacy_ward_code, 'UNKNOWN'),
    province = COALESCE(province, legacy_province, 'Chưa cập nhật'),
    province_code = COALESCE(province_code, 'UNKNOWN'),
    latitude = COALESCE(latitude, 0),
    longitude = COALESCE(longitude, 0);

ALTER TABLE fields
    ALTER COLUMN ward SET NOT NULL,
    ALTER COLUMN ward_code SET NOT NULL,
    ALTER COLUMN province SET NOT NULL,
    ALTER COLUMN province_code SET NOT NULL,
    ALTER COLUMN legacy_ward SET NOT NULL,
    ALTER COLUMN legacy_ward_code SET NOT NULL,
    ALTER COLUMN legacy_district SET NOT NULL,
    ALTER COLUMN legacy_province SET NOT NULL,
    ALTER COLUMN latitude SET NOT NULL,
    ALTER COLUMN longitude SET NOT NULL;

ALTER TABLE fields DROP COLUMN IF EXISTS district;
ALTER TABLE fields DROP COLUMN IF EXISTS city;

CREATE INDEX IF NOT EXISTS idx_fields_province_code ON fields(province_code);
CREATE INDEX IF NOT EXISTS idx_fields_ward_code ON fields(ward_code);
CREATE INDEX IF NOT EXISTS idx_fields_legacy_ward_code ON fields(legacy_ward_code);
