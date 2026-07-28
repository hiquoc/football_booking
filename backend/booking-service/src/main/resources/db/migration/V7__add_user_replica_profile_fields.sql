CREATE TABLE IF NOT EXISTS user_replicas (
    user_id UUID PRIMARY KEY,
    full_name VARCHAR(100),
    phone_number VARCHAR(20),
    avatar_url VARCHAR(1000),
    completed_booking_count INTEGER NOT NULL DEFAULT 0,
    total_matches INTEGER NOT NULL DEFAULT 0,
    wins INTEGER NOT NULL DEFAULT 0,
    losses INTEGER NOT NULL DEFAULT 0,
    draws INTEGER NOT NULL DEFAULT 0,
    no_cancel_rate NUMERIC(5, 2) NOT NULL DEFAULT 100,
    on_time_rate NUMERIC(5, 2) NOT NULL DEFAULT 100,
    fair_play_rate NUMERIC(5, 2) NOT NULL DEFAULT 100,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

ALTER TABLE user_replicas ADD COLUMN IF NOT EXISTS full_name VARCHAR(100);
ALTER TABLE user_replicas ADD COLUMN IF NOT EXISTS phone_number VARCHAR(20);
ALTER TABLE user_replicas ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(1000);
