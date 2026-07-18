ALTER TABLE bookings
    ADD COLUMN IF NOT EXISTS platform_booking_fee BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS sub_field_price NUMERIC(10, 2),
    ADD COLUMN IF NOT EXISTS booking_price BIGINT NOT NULL DEFAULT 0;

UPDATE bookings
SET sub_field_price = COALESCE(sub_field_price, total_amount),
    booking_price = COALESCE(NULLIF(booking_price, 0), platform_booking_fee, 0)
WHERE sub_field_price IS NULL OR booking_price = 0;

ALTER TABLE bookings
    ALTER COLUMN sub_field_price SET NOT NULL,
    ADD CONSTRAINT chk_bookings_booking_price_non_negative CHECK (booking_price >= 0);

CREATE TABLE user_replicas (
    user_id UUID PRIMARY KEY,
    completed_booking_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE field_violations (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    field_id UUID NOT NULL,
    violation_count INTEGER NOT NULL DEFAULT 0,
    is_banned BOOLEAN NOT NULL DEFAULT FALSE,
    ban_date TIMESTAMP,
    last_violation_date TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_field_violation_user_field UNIQUE (user_id, field_id)
);

CREATE TABLE booking_no_show_reports (
    id UUID PRIMARY KEY,
    booking_id UUID NOT NULL,
    field_id UUID NOT NULL,
    reported_user_id UUID NOT NULL,
    owner_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_no_show_report_booking UNIQUE (booking_id)
);

CREATE TABLE payment_dispute_reports (
    id UUID PRIMARY KEY,
    booking_id UUID NOT NULL,
    field_id UUID NOT NULL,
    reported_user_id UUID NOT NULL,
    owner_id UUID NOT NULL,
    description VARCHAR(1000) NOT NULL,
    status VARCHAR(20) NOT NULL,
    admin_note VARCHAR(1000),
    reviewed_at TIMESTAMP,
    reviewed_by UUID,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE payment_dispute_report_images (
    report_id UUID NOT NULL REFERENCES payment_dispute_reports(id),
    image_url VARCHAR(1000) NOT NULL
);

CREATE TABLE moderation_audit_logs (
    id UUID PRIMARY KEY,
    actor_id UUID,
    target_user_id UUID,
    field_id UUID,
    action VARCHAR(80) NOT NULL,
    details VARCHAR(1000),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE platform_bans (
    user_id UUID PRIMARY KEY,
    reason VARCHAR(1000) NOT NULL,
    banned_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_field_violations_field_banned ON field_violations(field_id, is_banned);
CREATE INDEX idx_field_violations_user_banned ON field_violations(user_id, is_banned);
CREATE INDEX idx_payment_disputes_owner ON payment_dispute_reports(owner_id);
CREATE INDEX idx_payment_disputes_status ON payment_dispute_reports(status);
CREATE INDEX idx_platform_bans_deleted ON platform_bans(deleted);
