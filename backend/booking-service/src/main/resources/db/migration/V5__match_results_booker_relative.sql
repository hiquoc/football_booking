CREATE TABLE IF NOT EXISTS match_results (
    id UUID PRIMARY KEY,
    booking_id UUID NOT NULL,
    winning_team VARCHAR(20) NOT NULL,
    team_a_percentage INTEGER NOT NULL,
    team_b_percentage INTEGER NOT NULL,
    team_a_amount NUMERIC(12, 2) NOT NULL,
    team_b_amount NUMERIC(12, 2) NOT NULL,
    submitted_by UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_match_results_booking UNIQUE (booking_id),
    CONSTRAINT fk_match_results_booking FOREIGN KEY (booking_id) REFERENCES bookings(id),
    CONSTRAINT chk_match_results_winning_team CHECK (winning_team IN ('TEAM_A', 'TEAM_B', 'BOOKER_WIN', 'BOOKER_LOSS', 'DRAW'))
);

CREATE INDEX IF NOT EXISTS idx_match_results_booking
    ON match_results(booking_id);

UPDATE match_results
SET winning_team = CASE winning_team
    WHEN 'TEAM_A' THEN 'BOOKER_WIN'
    WHEN 'TEAM_B' THEN 'BOOKER_LOSS'
    ELSE winning_team
END
WHERE winning_team IN ('TEAM_A', 'TEAM_B');
