ALTER TABLE field_operating_hours_projections
    ADD COLUMN IF NOT EXISTS open_24_hours BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE sub_field_operating_hours_projections
    ADD COLUMN IF NOT EXISTS open_24_hours BOOLEAN NOT NULL DEFAULT FALSE;
