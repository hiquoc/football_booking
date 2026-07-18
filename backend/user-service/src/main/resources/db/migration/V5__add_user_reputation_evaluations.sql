CREATE TABLE user_reputation_evaluations (
    id UUID PRIMARY KEY,
    post_id UUID NOT NULL,
    booking_id UUID NOT NULL,
    evaluator_id UUID NOT NULL,
    evaluated_user_id UUID NOT NULL REFERENCES users(id),
    arrived_on_time BOOLEAN NOT NULL,
    cancelled_unexpectedly BOOLEAN NOT NULL,
    fair_play BOOLEAN NOT NULL,
    would_play_again BOOLEAN NOT NULL,
    comment VARCHAR(1000),
    occurred_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_user_reputation_evaluations_user ON user_reputation_evaluations(evaluated_user_id);
