CREATE INDEX IF NOT EXISTS idx_reviews_field_created_at
    ON reviews(field_id, created_at DESC)
    WHERE deleted = false;
