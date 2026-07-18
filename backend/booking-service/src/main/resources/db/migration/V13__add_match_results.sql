CREATE TABLE match_results (
    id UUID PRIMARY KEY,
    booking_id UUID NOT NULL,
    winning_team VARCHAR(20) NOT NULL,
    team_a_percentage INTEGER NOT NULL,
    team_b_percentage INTEGER NOT NULL,
    team_a_amount NUMERIC(12, 2) NOT NULL,
    team_b_amount NUMERIC(12, 2) NOT NULL,
    submitted_by UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_match_results_booking UNIQUE (booking_id),
    CONSTRAINT chk_match_results_winning_team CHECK (winning_team IN ('TEAM_A', 'TEAM_B', 'DRAW')),
    CONSTRAINT chk_match_results_split_total CHECK (team_a_percentage + team_b_percentage = 100),
    CONSTRAINT chk_match_results_split_range CHECK (
        team_a_percentage BETWEEN 0 AND 100
        AND team_b_percentage BETWEEN 0 AND 100
    )
);

CREATE INDEX idx_match_results_booking ON match_results(booking_id);

ALTER TABLE user_replicas
    ADD COLUMN IF NOT EXISTS total_matches INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS wins INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS losses INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS draws INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS no_cancel_rate NUMERIC(5, 2) NOT NULL DEFAULT 100,
    ADD COLUMN IF NOT EXISTS on_time_rate NUMERIC(5, 2) NOT NULL DEFAULT 100,
    ADD COLUMN IF NOT EXISTS fair_play_rate NUMERIC(5, 2) NOT NULL DEFAULT 100;
