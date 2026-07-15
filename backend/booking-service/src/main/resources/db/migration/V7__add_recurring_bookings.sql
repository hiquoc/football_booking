ALTER TABLE sub_field_projections
    ADD COLUMN has_recurring BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE bookings
    ADD COLUMN source_recurring_booking_id UUID;

CREATE TABLE recurring_bookings (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    field_id UUID NOT NULL,
    sub_field_id UUID NOT NULL REFERENCES sub_field_projections(id),
    day_of_week VARCHAR(20) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    next_process_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_recurring_booking_time CHECK (start_time < end_time),
    CONSTRAINT ck_recurring_booking_date CHECK (start_date <= end_date)
);

CREATE INDEX idx_recurring_bookings_sub_field_day ON recurring_bookings(sub_field_id, day_of_week);
CREATE INDEX idx_recurring_bookings_next_process_at ON recurring_bookings(next_process_at);
CREATE INDEX idx_recurring_bookings_status ON recurring_bookings(status);
CREATE INDEX idx_recurring_bookings_user ON recurring_bookings(user_id);
CREATE INDEX idx_recurring_bookings_field ON recurring_bookings(field_id);
CREATE INDEX idx_bookings_source_recurring_date ON bookings(source_recurring_booking_id, booking_date);
