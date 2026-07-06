CREATE INDEX IF NOT EXISTS idx_fields_public_rating
    ON fields (rating_average DESC, id) WHERE status = 'APPROVED' AND active = true AND deleted = false;

CREATE INDEX IF NOT EXISTS idx_fields_public_reviews
    ON fields (total_reviews DESC, id) WHERE status = 'APPROVED' AND active = true AND deleted = false;

CREATE INDEX IF NOT EXISTS idx_fields_public_location
    ON fields (province_code, ward_code) WHERE status = 'APPROVED' AND active = true AND deleted = false;

CREATE INDEX IF NOT EXISTS idx_field_images_primary
    ON field_images (field_id, is_primary DESC, display_order, id);

CREATE INDEX IF NOT EXISTS idx_sub_fields_search_type
    ON sub_fields (sub_field_type, field_id) WHERE active = true AND deleted = false;

CREATE INDEX IF NOT EXISTS idx_field_field_types_reverse
    ON field_field_types (field_type_id, field_id);
