CREATE TABLE booking_config (
    id UUID PRIMARY KEY,
    first_booking_fee BIGINT NOT NULL CHECK (first_booking_fee >= 0),
    not_first_booking_fee BIGINT NOT NULL CHECK (not_first_booking_fee >= 0),
    refund_before_hours INTEGER NOT NULL CHECK (refund_before_hours >= 0),
    refund_enabled BOOLEAN NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_booking_config_single_active UNIQUE (active)
);

INSERT INTO booking_config (id, first_booking_fee, not_first_booking_fee, refund_before_hours, refund_enabled, active, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000001', 5000, 1000, 24, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

ALTER TABLE bookings ADD COLUMN payment_method VARCHAR(30) NOT NULL DEFAULT 'STRIPE';
