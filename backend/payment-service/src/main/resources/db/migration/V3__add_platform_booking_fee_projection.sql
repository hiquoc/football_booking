ALTER TABLE booking_payment_projections
    ADD COLUMN IF NOT EXISTS platform_booking_fee BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS sub_field_price NUMERIC(19, 2),
    ADD COLUMN IF NOT EXISTS booking_price BIGINT NOT NULL DEFAULT 0;

UPDATE booking_payment_projections
SET sub_field_price = COALESCE(sub_field_price, total_amount),
    booking_price = COALESCE(NULLIF(booking_price, 0), platform_booking_fee, 0)
WHERE sub_field_price IS NULL OR booking_price = 0;

ALTER TABLE booking_payment_projections
    ALTER COLUMN sub_field_price SET NOT NULL;

ALTER TABLE booking_payment_projections
    ADD CONSTRAINT chk_booking_payment_projection_platform_booking_fee_non_negative CHECK (platform_booking_fee >= 0);
