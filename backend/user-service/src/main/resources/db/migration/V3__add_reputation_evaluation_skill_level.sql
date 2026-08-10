ALTER TABLE user_reputation_evaluations
    ADD COLUMN IF NOT EXISTS skill_level VARCHAR(40) NOT NULL DEFAULT 'AVERAGE',
    ADD COLUMN IF NOT EXISTS source_evaluation_id UUID;

CREATE UNIQUE INDEX IF NOT EXISTS uk_user_reputation_evaluations_source
    ON user_reputation_evaluations(source_evaluation_id)
    WHERE source_evaluation_id IS NOT NULL;
