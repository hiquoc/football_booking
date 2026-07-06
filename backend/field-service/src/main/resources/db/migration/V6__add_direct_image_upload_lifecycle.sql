ALTER TABLE field_images ALTER COLUMN image_url DROP NOT NULL;
ALTER TABLE field_images ADD COLUMN public_id VARCHAR(255);
ALTER TABLE field_images ADD COLUMN upload_owner_id UUID;
ALTER TABLE field_images ADD COLUMN upload_request_id UUID;
ALTER TABLE field_images ADD COLUMN upload_slot_index INTEGER;
ALTER TABLE field_images ADD COLUMN upload_timestamp BIGINT;
ALTER TABLE field_images ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE field_images ADD COLUMN confirmed_at TIMESTAMP;
ALTER TABLE field_images ADD COLUMN asset_version BIGINT;
ALTER TABLE field_images ADD COLUMN image_format VARCHAR(32);
ALTER TABLE field_images ADD COLUMN width INTEGER;
ALTER TABLE field_images ADD COLUMN height INTEGER;
ALTER TABLE field_images ADD COLUMN byte_size BIGINT;

CREATE TABLE issued_image_public_ids (
    public_id VARCHAR(255) PRIMARY KEY,
    issued_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_field_images_public_id ON field_images(public_id) WHERE public_id IS NOT NULL;
CREATE UNIQUE INDEX uk_field_images_upload_request_slot
    ON field_images(field_id, upload_owner_id, upload_request_id, upload_slot_index)
    WHERE upload_request_id IS NOT NULL;
CREATE INDEX idx_field_images_stale_placeholder ON field_images(created_at) WHERE image_url IS NULL;
