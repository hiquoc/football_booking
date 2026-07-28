ALTER TABLE IF EXISTS user_replicas RENAME TO user_projections;

ALTER INDEX IF EXISTS idx_user_replicas_completed_booking_count RENAME TO idx_user_projections_completed_booking_count;

ALTER TABLE user_projections ADD COLUMN IF NOT EXISTS bio VARCHAR(500);
ALTER TABLE user_projections ADD COLUMN IF NOT EXISTS team_photo_url VARCHAR(1000);
ALTER TABLE user_projections ADD COLUMN IF NOT EXISTS skill_level VARCHAR(30);
ALTER TABLE user_projections ADD COLUMN IF NOT EXISTS email VARCHAR(100);
ALTER TABLE user_projections ADD COLUMN IF NOT EXISTS user_type VARCHAR(20);
ALTER TABLE user_projections ADD COLUMN IF NOT EXISTS status VARCHAR(20);
ALTER TABLE user_projections ADD COLUMN IF NOT EXISTS balance BIGINT NOT NULL DEFAULT 0;
