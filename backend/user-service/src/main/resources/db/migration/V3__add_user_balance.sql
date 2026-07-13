ALTER TABLE users ADD COLUMN balance BIGINT NOT NULL DEFAULT 0 CHECK (balance >= 0);

CREATE TABLE user_balance_transactions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    booking_id UUID NOT NULL,
    operation_key VARCHAR(120) NOT NULL UNIQUE,
    type VARCHAR(30) NOT NULL,
    amount BIGINT NOT NULL CHECK (amount > 0),
    reason VARCHAR(80) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_user_balance_transactions_user ON user_balance_transactions(user_id);
CREATE INDEX idx_user_balance_transactions_booking ON user_balance_transactions(booking_id);
