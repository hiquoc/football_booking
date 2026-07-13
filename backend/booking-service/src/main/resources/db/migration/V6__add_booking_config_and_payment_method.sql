CREATE TABLE booking_config (
    id UUID PRIMARY KEY,
    booking_fee BIGINT NOT NULL CHECK (booking_fee >= 0),
    refund_before_hours INTEGER NOT NULL CHECK (refund_before_hours >= 0),
    refund_enabled BOOLEAN NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_booking_config_single_active UNIQUE (active)
);

INSERT INTO booking_config (id, booking_fee, refund_before_hours, refund_enabled, active, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000001', 0, 24, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

ALTER TABLE bookings ADD COLUMN payment_method VARCHAR(30) NOT NULL DEFAULT 'STRIPE';
