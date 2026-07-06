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
