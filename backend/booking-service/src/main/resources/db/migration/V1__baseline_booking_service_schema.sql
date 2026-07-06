CREATE TABLE sub_field_projections (
    id UUID PRIMARY KEY,
    field_id UUID NOT NULL,
    field_name VARCHAR(255),
    name VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL,
    owner_id UUID NOT NULL,
    sub_field_type VARCHAR(50),
    minimum_booking_duration_minutes INTEGER,
    maximum_booking_duration_minutes INTEGER,
    booking_interval_minutes INTEGER
);

CREATE TABLE bookings (
    id UUID PRIMARY KEY,
    booking_code VARCHAR(50) NOT NULL UNIQUE,
    client_id UUID NOT NULL,
    sub_field_id UUID NOT NULL REFERENCES sub_field_projections(id),
    owner_id UUID NOT NULL,
    booking_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    duration_minutes INTEGER NOT NULL,
    price_per_hour NUMERIC(10, 2) NOT NULL,
    total_amount NUMERIC(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    note VARCHAR(255),
    cancellation_reason VARCHAR(255),
    cancelled_at TIMESTAMP,
    cancelled_by VARCHAR(20),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE field_operating_hours_projections (
    id UUID PRIMARY KEY,
    field_id UUID NOT NULL,
    day_of_week VARCHAR(20) NOT NULL,
    open_time TIME,
    close_time TIME,
    closed BOOLEAN NOT NULL,
    CONSTRAINT uk_field_hours_projection_day UNIQUE (field_id, day_of_week)
);

CREATE TABLE sub_field_operating_hours_projections (
    id UUID PRIMARY KEY,
    sub_field_id UUID NOT NULL,
    day_of_week VARCHAR(20) NOT NULL,
    open_time TIME,
    close_time TIME,
    closed BOOLEAN NOT NULL,
    CONSTRAINT uk_sub_field_hours_projection_day UNIQUE (sub_field_id, day_of_week)
);

CREATE TABLE sub_field_closure_projections (
    id UUID PRIMARY KEY,
    sub_field_id UUID NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    reason VARCHAR(255)
);

CREATE TABLE time_price_rule_projections (
    id UUID PRIMARY KEY,
    sub_field_id UUID NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    hourly_price NUMERIC(10, 2) NOT NULL
);

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(255) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    topic VARCHAR(255) NOT NULL,
    event_key VARCHAR(255) NOT NULL,
    payload OID NOT NULL,
    headers OID NOT NULL,
    status VARCHAR(255) NOT NULL,
    retry_count INTEGER NOT NULL,
    next_retry_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ,
    error_message OID
);

CREATE INDEX idx_outbox_status ON outbox_events(status);
CREATE INDEX idx_outbox_next_retry_at ON outbox_events(next_retry_at);
CREATE INDEX idx_outbox_created_at ON outbox_events(created_at);

CREATE TABLE inbox_events (
    id UUID PRIMARY KEY,
    event_id VARCHAR(255) NOT NULL,
    consumer_group VARCHAR(255) NOT NULL,
    topic VARCHAR(255) NOT NULL,
    kafka_partition INTEGER NOT NULL,
    kafka_offset BIGINT NOT NULL,
    payload_type VARCHAR(255) NOT NULL,
    payload OID NOT NULL,
    status VARCHAR(255) NOT NULL,
    retry_count INTEGER NOT NULL,
    next_retry_at TIMESTAMPTZ NOT NULL,
    received_at TIMESTAMPTZ NOT NULL,
    processed_at TIMESTAMPTZ,
    error_message OID,
    CONSTRAINT uk_inbox_event_consumer UNIQUE (event_id, consumer_group)
);

CREATE INDEX idx_inbox_status ON inbox_events(status);
CREATE INDEX idx_inbox_next_retry_at ON inbox_events(next_retry_at);

CREATE INDEX idx_bookings_client ON bookings(client_id);
CREATE INDEX idx_bookings_owner ON bookings(owner_id);
CREATE INDEX idx_bookings_sub_field_date ON bookings(sub_field_id, booking_date);
CREATE INDEX idx_bookings_status ON bookings(status);
CREATE INDEX idx_sub_field_projections_field ON sub_field_projections(field_id);
CREATE INDEX idx_field_hours_projection_field ON field_operating_hours_projections(field_id);
CREATE INDEX idx_sub_field_hours_projection_sub_field ON sub_field_operating_hours_projections(sub_field_id);
CREATE INDEX idx_sub_field_closure_projection_sub_field ON sub_field_closure_projections(sub_field_id);
CREATE INDEX idx_time_price_rule_projection_sub_field ON time_price_rule_projections(sub_field_id);
