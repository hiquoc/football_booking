CREATE TABLE field_favorites (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    field_id UUID NOT NULL REFERENCES fields(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_field_favorites_user_field UNIQUE (user_id, field_id)
);

CREATE INDEX idx_field_favorites_user ON field_favorites(user_id);
CREATE INDEX idx_field_favorites_field ON field_favorites(field_id);
CREATE INDEX idx_field_favorites_user_field_active ON field_favorites(user_id, field_id) WHERE deleted = false;
