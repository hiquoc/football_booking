UPDATE match_results
SET winning_team = CASE winning_team
    WHEN 'TEAM_A' THEN 'BOOKER_WIN'
    WHEN 'TEAM_B' THEN 'BOOKER_LOSS'
    ELSE winning_team
END
WHERE winning_team IN ('TEAM_A', 'TEAM_B');
