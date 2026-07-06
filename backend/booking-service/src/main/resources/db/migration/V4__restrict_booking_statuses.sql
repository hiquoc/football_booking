UPDATE bookings
SET status = 'CANCELLED',
    cancellation_reason = COALESCE(cancellation_reason, 'Payment timeout'),
    cancelled_at = COALESCE(cancelled_at, updated_at, NOW()),
    cancelled_by = COALESCE(cancelled_by, 'SYSTEM')
WHERE status = 'EXPIRED';

UPDATE bookings
SET status = CASE
    WHEN booking_date + end_time <= NOW() THEN 'COMPLETED'
    ELSE 'CONFIRMED'
END
WHERE status = 'IN_PROGRESS';

ALTER TABLE bookings
    DROP CONSTRAINT IF EXISTS chk_bookings_status;

ALTER TABLE bookings
    ADD CONSTRAINT chk_bookings_status
    CHECK (status IN ('PENDING', 'CONFIRMED', 'COMPLETED', 'CANCELLED'));
