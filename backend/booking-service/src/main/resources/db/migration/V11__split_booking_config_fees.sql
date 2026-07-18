ALTER TABLE booking_config
    ADD COLUMN IF NOT EXISTS first_booking_fee BIGINT,
    ADD COLUMN IF NOT EXISTS not_first_booking_fee BIGINT;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'booking_config'
          AND column_name = 'booking_fee'
    ) THEN
        UPDATE booking_config
        SET first_booking_fee = COALESCE(first_booking_fee, NULLIF(booking_fee, 0), 5000),
            not_first_booking_fee = COALESCE(not_first_booking_fee, 1000),
            updated_at = CURRENT_TIMESTAMP;
    ELSE
        UPDATE booking_config
        SET first_booking_fee = COALESCE(first_booking_fee, 5000),
            not_first_booking_fee = COALESCE(not_first_booking_fee, 1000),
            updated_at = CURRENT_TIMESTAMP;
    END IF;
END $$;

ALTER TABLE booking_config
    ALTER COLUMN first_booking_fee SET NOT NULL,
    ALTER COLUMN not_first_booking_fee SET NOT NULL,
    ADD CONSTRAINT chk_booking_config_first_booking_fee_non_negative CHECK (first_booking_fee >= 0),
    ADD CONSTRAINT chk_booking_config_not_first_booking_fee_non_negative CHECK (not_first_booking_fee >= 0);

ALTER TABLE booking_config
    DROP COLUMN IF EXISTS booking_fee;
