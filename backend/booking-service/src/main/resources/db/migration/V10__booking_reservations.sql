ALTER TABLE bookings
    ADD COLUMN IF NOT EXISTS booking_type VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    ADD COLUMN IF NOT EXISTS payment_method VARCHAR(30) NOT NULL DEFAULT 'ACCOUNT_BALANCE',
    ADD COLUMN IF NOT EXISTS payment_status VARCHAR(20) NOT NULL DEFAULT 'UNPAID',
    ADD COLUMN IF NOT EXISTS payment_expires_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS platform_booking_fee BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS start_date_time TIMESTAMP,
    ADD COLUMN IF NOT EXISTS end_date_time TIMESTAMP;

UPDATE bookings
SET start_date_time = COALESCE(start_date_time, booking_date + start_time),
    end_date_time = COALESCE(
            end_date_time,
            CASE
                WHEN end_time > start_time THEN booking_date + end_time
                ELSE booking_date + end_time + INTERVAL '1 day'
            END)
WHERE start_date_time IS NULL
   OR end_date_time IS NULL;

ALTER TABLE bookings
    ALTER COLUMN start_date_time SET NOT NULL,
    ALTER COLUMN end_date_time SET NOT NULL;

ALTER TABLE bookings
    DROP CONSTRAINT IF EXISTS chk_bookings_booking_type,
    ADD CONSTRAINT chk_bookings_booking_type CHECK (booking_type IN ('NORMAL', 'RESERVATION'));

ALTER TABLE bookings
    DROP CONSTRAINT IF EXISTS chk_bookings_payment_status,
    ADD CONSTRAINT chk_bookings_payment_status CHECK (payment_status IN ('UNPAID', 'PAID', 'NOT_REQUIRED', 'REFUNDED', 'FAILED'));

DROP INDEX IF EXISTS idx_bookings_owner_reservations;
CREATE INDEX idx_bookings_owner_reservations
    ON bookings(owner_id, booking_type, start_date_time);
