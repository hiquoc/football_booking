ALTER TABLE bookings
    ADD COLUMN IF NOT EXISTS sub_field_price NUMERIC(10, 2),
    ADD COLUMN IF NOT EXISTS booking_price BIGINT NOT NULL DEFAULT 0;

UPDATE bookings
SET sub_field_price = COALESCE(sub_field_price, total_amount),
    booking_price = COALESCE(NULLIF(booking_price, 0), platform_booking_fee, 0)
WHERE sub_field_price IS NULL OR booking_price = 0;

ALTER TABLE bookings
    ALTER COLUMN sub_field_price SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_bookings_booking_price_non_negative'
    ) THEN
        ALTER TABLE bookings
            ADD CONSTRAINT chk_bookings_booking_price_non_negative CHECK (booking_price >= 0);
    END IF;
END $$;
