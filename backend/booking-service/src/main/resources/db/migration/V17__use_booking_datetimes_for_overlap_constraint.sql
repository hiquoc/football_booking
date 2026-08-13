CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE bookings
    DROP CONSTRAINT IF EXISTS bookings_no_overlapping_active_bookings;

ALTER TABLE bookings
    ADD CONSTRAINT bookings_no_overlapping_active_bookings
    EXCLUDE USING gist (
        sub_field_id WITH =,
        tsrange(start_date_time, end_date_time, '[)') WITH &&
    )
    WHERE (status IN ('PENDING', 'CONFIRMED') AND deleted = FALSE);
