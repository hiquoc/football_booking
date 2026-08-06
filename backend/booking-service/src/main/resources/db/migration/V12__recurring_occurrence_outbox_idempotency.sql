CREATE UNIQUE INDEX IF NOT EXISTS uk_outbox_recurring_occurrence_command
    ON outbox_events(event_type, aggregate_type, aggregate_id)
    WHERE event_type = 'RECURRING_OCCURRENCE_REQUESTED';
