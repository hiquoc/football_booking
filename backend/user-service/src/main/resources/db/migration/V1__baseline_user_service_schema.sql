CREATE TABLE users (
    id UUID PRIMARY KEY, phone_number VARCHAR(20) UNIQUE, email VARCHAR(100) UNIQUE,
    full_name VARCHAR(100) NOT NULL, avatar_url VARCHAR(255), user_type VARCHAR(20) NOT NULL,
    social_provider VARCHAR(20), social_provider_id VARCHAR(100), status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL, created_by VARCHAR(255),
    updated_by VARCHAR(255), deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY, aggregate_type VARCHAR(255) NOT NULL, aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL, topic VARCHAR(255) NOT NULL, event_key VARCHAR(255) NOT NULL,
    payload OID NOT NULL, headers OID NOT NULL, status VARCHAR(255) NOT NULL, retry_count INTEGER NOT NULL,
    next_retry_at TIMESTAMPTZ NOT NULL, created_at TIMESTAMPTZ NOT NULL, published_at TIMESTAMPTZ, error_message OID
);
CREATE INDEX idx_outbox_status ON outbox_events(status);
CREATE INDEX idx_outbox_next_retry_at ON outbox_events(next_retry_at);
CREATE INDEX idx_outbox_created_at ON outbox_events(created_at);

CREATE TABLE inbox_events (
    id UUID PRIMARY KEY, event_id VARCHAR(255) NOT NULL, consumer_group VARCHAR(255) NOT NULL,
    topic VARCHAR(255) NOT NULL, kafka_partition INTEGER NOT NULL, kafka_offset BIGINT NOT NULL,
    payload_type VARCHAR(255) NOT NULL, payload OID NOT NULL, status VARCHAR(255) NOT NULL,
    retry_count INTEGER NOT NULL, next_retry_at TIMESTAMPTZ NOT NULL, received_at TIMESTAMPTZ NOT NULL,
    processed_at TIMESTAMPTZ, error_message OID,
    CONSTRAINT uk_inbox_event_consumer UNIQUE (event_id, consumer_group)
);
CREATE INDEX idx_inbox_status ON inbox_events(status);
CREATE INDEX idx_inbox_next_retry_at ON inbox_events(next_retry_at);
