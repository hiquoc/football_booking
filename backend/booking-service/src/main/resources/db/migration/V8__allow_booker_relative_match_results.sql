ALTER TABLE match_results
    DROP CONSTRAINT IF EXISTS chk_match_results_winning_team;

UPDATE match_results
SET winning_team = CASE winning_team
    WHEN 'TEAM_A' THEN 'BOOKER_WIN'
    WHEN 'TEAM_B' THEN 'BOOKER_LOSS'
    ELSE winning_team
END
WHERE winning_team IN ('TEAM_A', 'TEAM_B');

ALTER TABLE match_results
    ADD CONSTRAINT chk_match_results_winning_team
    CHECK (winning_team IN ('BOOKER_WIN', 'BOOKER_LOSS', 'DRAW'));
