ALTER TABLE bookings
    DROP CONSTRAINT IF EXISTS chk_bookings_status;

UPDATE bookings
SET status = 'EXPIRED'
WHERE status = 'CANCELLED'
  AND cancelled_by = 'SYSTEM'
  AND cancellation_reason = 'Payment timeout';

ALTER TABLE bookings
    ADD CONSTRAINT chk_bookings_status
    CHECK (status IN ('PENDING', 'CONFIRMED', 'COMPLETED', 'CANCELLED', 'EXPIRED'));
