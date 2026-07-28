CREATE EXTENSION IF NOT EXISTS btree_gist;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'bookings_no_overlapping_active_bookings'
    ) THEN
        ALTER TABLE bookings
            ADD CONSTRAINT bookings_no_overlapping_active_bookings
            EXCLUDE USING gist (
                sub_field_id WITH =,
                tsrange(booking_date + start_time, booking_date + end_time, '[)') WITH &&
            )
            WHERE (status IN ('PENDING', 'CONFIRMED') AND deleted = FALSE);
    END IF;
END $$;

ALTER TABLE bookings
    ADD COLUMN IF NOT EXISTS sub_field_price NUMERIC(10, 2),
    ADD COLUMN IF NOT EXISTS booking_price BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS source_recurring_booking_id UUID;

UPDATE bookings
SET sub_field_price = COALESCE(sub_field_price, total_amount)
WHERE sub_field_price IS NULL;

ALTER TABLE bookings
    ALTER COLUMN sub_field_price SET NOT NULL;

ALTER TABLE sub_field_projections
    ADD COLUMN IF NOT EXISTS has_recurring BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS recurring_bookings (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    field_id UUID NOT NULL,
    sub_field_id UUID NOT NULL REFERENCES sub_field_projections(id),
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    interval_days INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    next_process_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

ALTER TABLE recurring_bookings
    DROP COLUMN IF EXISTS day_of_week,
    ADD COLUMN IF NOT EXISTS interval_days INTEGER NOT NULL DEFAULT 7;

ALTER TABLE recurring_bookings
    DROP CONSTRAINT IF EXISTS ck_recurring_booking_time,
    ADD CONSTRAINT ck_recurring_booking_time CHECK (start_time < end_time);

ALTER TABLE recurring_bookings
    DROP CONSTRAINT IF EXISTS ck_recurring_booking_date,
    ADD CONSTRAINT ck_recurring_booking_date CHECK (start_date < end_date);

ALTER TABLE recurring_bookings
    DROP CONSTRAINT IF EXISTS ck_recurring_booking_interval,
    ADD CONSTRAINT ck_recurring_booking_interval CHECK (interval_days BETWEEN 1 AND 7);

ALTER TABLE recurring_bookings
    ALTER COLUMN interval_days DROP DEFAULT;

DROP INDEX IF EXISTS idx_recurring_bookings_sub_field_day;

CREATE INDEX IF NOT EXISTS idx_recurring_bookings_sub_field_dates
    ON recurring_bookings(sub_field_id, start_date, end_date);

CREATE INDEX IF NOT EXISTS idx_recurring_bookings_next_process_at
    ON recurring_bookings(next_process_at);

CREATE INDEX IF NOT EXISTS idx_recurring_bookings_status
    ON recurring_bookings(status);

CREATE INDEX IF NOT EXISTS idx_recurring_bookings_user
    ON recurring_bookings(user_id);

CREATE INDEX IF NOT EXISTS idx_recurring_bookings_field
    ON recurring_bookings(field_id);

CREATE INDEX IF NOT EXISTS idx_bookings_source_recurring_date
    ON bookings(source_recurring_booking_id, booking_date);
