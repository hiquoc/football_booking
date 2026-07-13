CREATE TABLE booking_payment_projections (
    booking_id UUID PRIMARY KEY, booking_code VARCHAR(50) NOT NULL, user_id UUID NOT NULL,
    user_email VARCHAR(255), total_amount NUMERIC(19,2) NOT NULL CHECK (total_amount > 0)
);
CREATE TABLE payments (
    id UUID PRIMARY KEY, booking_id UUID NOT NULL UNIQUE, user_id UUID NOT NULL,
    provider VARCHAR(30) NOT NULL CHECK (provider IN ('STRIPE')),
    stripe_session_id VARCHAR(255) UNIQUE, payment_intent_id VARCHAR(255) UNIQUE,
    amount NUMERIC(19,2) NOT NULL CHECK (amount > 0), currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING','SUCCESS','FAILED','CANCELLED')),
    failure_reason VARCHAR(500), checkout_attempt INTEGER NOT NULL DEFAULT 0, version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255), updated_by VARCHAR(255), deleted BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_payments_user ON payments(user_id);
CREATE TABLE payment_sessions (
    provider_session_id VARCHAR(255) PRIMARY KEY, payment_id UUID NOT NULL REFERENCES payments(id), attempt INTEGER NOT NULL,
    CONSTRAINT uk_payment_session_attempt UNIQUE(payment_id, attempt)
);
CREATE TABLE provider_webhook_events (
    event_id VARCHAR(255) PRIMARY KEY, provider VARCHAR(30) NOT NULL,
    event_type VARCHAR(100) NOT NULL, processed_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE outbox_events (
    id UUID PRIMARY KEY, aggregate_type VARCHAR(255) NOT NULL, aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL, topic VARCHAR(255) NOT NULL, event_key VARCHAR(255) NOT NULL,
    payload OID NOT NULL, headers OID NOT NULL, status VARCHAR(255) NOT NULL, retry_count INTEGER NOT NULL,
    next_retry_at TIMESTAMPTZ NOT NULL, created_at TIMESTAMPTZ NOT NULL, published_at TIMESTAMPTZ, error_message OID
);
CREATE INDEX idx_payment_outbox_status ON outbox_events(status);
CREATE INDEX idx_payment_outbox_next_retry ON outbox_events(next_retry_at);
CREATE INDEX idx_payment_outbox_created ON outbox_events(created_at);
CREATE TABLE inbox_events (
    id UUID PRIMARY KEY, event_id VARCHAR(255) NOT NULL, consumer_group VARCHAR(255) NOT NULL,
    topic VARCHAR(255) NOT NULL, kafka_partition INTEGER NOT NULL, kafka_offset BIGINT NOT NULL,
    payload_type VARCHAR(255) NOT NULL, payload OID NOT NULL, status VARCHAR(255) NOT NULL,
    retry_count INTEGER NOT NULL, next_retry_at TIMESTAMPTZ NOT NULL, received_at TIMESTAMPTZ NOT NULL,
    processed_at TIMESTAMPTZ, error_message OID, CONSTRAINT uk_payment_inbox_event UNIQUE(event_id, consumer_group)
);
CREATE INDEX idx_payment_inbox_status ON inbox_events(status);
CREATE INDEX idx_payment_inbox_next_retry ON inbox_events(next_retry_at);
