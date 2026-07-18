ALTER TABLE users
    ADD COLUMN IF NOT EXISTS completed_booking_count INTEGER NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_users_status ON users(status);
