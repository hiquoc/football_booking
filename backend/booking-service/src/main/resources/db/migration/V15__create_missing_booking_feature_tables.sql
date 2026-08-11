CREATE TABLE IF NOT EXISTS booking_config (
    id UUID PRIMARY KEY,
    first_booking_fee BIGINT NOT NULL,
    not_first_booking_fee BIGINT NOT NULL,
    refund_before_hours INTEGER NOT NULL,
    refund_enabled BOOLEAN NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS booking_no_show_reports (
    id UUID PRIMARY KEY,
    booking_id UUID NOT NULL,
    field_id UUID NOT NULL,
    reported_user_id UUID NOT NULL,
    owner_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_no_show_report_booking UNIQUE (booking_id)
);

CREATE TABLE IF NOT EXISTS platform_bans (
    user_id UUID PRIMARY KEY,
    reason VARCHAR(1000) NOT NULL,
    banned_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS payment_dispute_reports (
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
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS payment_dispute_report_images (
    report_id UUID NOT NULL,
    image_url VARCHAR(1000) NOT NULL,
    CONSTRAINT fk_payment_dispute_report_images_report
        FOREIGN KEY (report_id) REFERENCES payment_dispute_reports(id)
);

CREATE TABLE IF NOT EXISTS moderation_audit_logs (
    id UUID PRIMARY KEY,
    actor_id UUID,
    target_user_id UUID,
    field_id UUID,
    action VARCHAR(80) NOT NULL,
    details VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS field_violations (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    field_id UUID NOT NULL,
    violation_count INTEGER NOT NULL DEFAULT 0,
    is_banned BOOLEAN NOT NULL DEFAULT FALSE,
    ban_date TIMESTAMP,
    last_violation_date TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_field_violation_user_field UNIQUE (user_id, field_id)
);

CREATE TABLE IF NOT EXISTS community_posts (
    id UUID PRIMARY KEY,
    booking_id UUID NOT NULL,
    owner_id UUID NOT NULL,
    post_type VARCHAR(40) NOT NULL,
    status VARCHAR(30) NOT NULL,
    title VARCHAR(120) NOT NULL,
    description VARCHAR(2000),
    skill_level VARCHAR(40) NOT NULL,
    contact_phone VARCHAR(20) NOT NULL,
    players_needed INTEGER,
    accepted_players_count INTEGER NOT NULL DEFAULT 0,
    booking_code VARCHAR(50) NOT NULL,
    field_id UUID,
    field_owner_id UUID,
    field_name VARCHAR(255),
    sub_field_id UUID NOT NULL,
    sub_field_name VARCHAR(255),
    field_type VARCHAR(50),
    booking_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    owner_display_name VARCHAR(255),
    owner_avatar_url VARCHAR(1000),
    owner_team_photo_url VARCHAR(1000),
    location_text VARCHAR(255),
    matched_application_id UUID,
    closed_at TIMESTAMP,
    hidden_at TIMESTAMP,
    hidden_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS community_applications (
    id UUID PRIMARY KEY,
    post_id UUID NOT NULL,
    applicant_id UUID NOT NULL,
    status VARCHAR(30) NOT NULL,
    message VARCHAR(1000),
    applicant_display_name VARCHAR(255),
    applicant_avatar_url VARCHAR(1000),
    applicant_team_photo_url VARCHAR(1000),
    applicant_skill_level VARCHAR(40),
    decided_at TIMESTAMP,
    withdrawn_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_community_applications_post
        FOREIGN KEY (post_id) REFERENCES community_posts(id)
);

CREATE TABLE IF NOT EXISTS community_post_reports (
    id UUID PRIMARY KEY,
    post_id UUID NOT NULL,
    reporter_id UUID NOT NULL,
    reason VARCHAR(50) NOT NULL,
    description VARCHAR(1000),
    status VARCHAR(30) NOT NULL,
    reviewed_by UUID,
    reviewed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_community_post_reports_post
        FOREIGN KEY (post_id) REFERENCES community_posts(id)
);

CREATE TABLE IF NOT EXISTS community_user_violations (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    reason VARCHAR(500) NOT NULL,
    action VARCHAR(50) NOT NULL,
    expire_at TIMESTAMP,
    status VARCHAR(30) NOT NULL,
    source_post_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS match_evaluations (
    id UUID PRIMARY KEY,
    post_id UUID NOT NULL,
    booking_id UUID NOT NULL,
    evaluator_id UUID NOT NULL,
    evaluated_user_id UUID NOT NULL,
    arrived_on_time BOOLEAN NOT NULL,
    cancelled_unexpectedly BOOLEAN NOT NULL,
    fair_play BOOLEAN NOT NULL,
    would_play_again BOOLEAN NOT NULL,
    skill_level VARCHAR(40) NOT NULL DEFAULT 'AVERAGE',
    comment VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS community_moderation_history (
    id UUID PRIMARY KEY,
    target_user_id UUID NOT NULL,
    target_post_id UUID,
    moderator_id UUID NOT NULL,
    action VARCHAR(60) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    note VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_community_posts_status
    ON community_posts(status);
CREATE INDEX IF NOT EXISTS idx_community_posts_booking
    ON community_posts(booking_id);
CREATE INDEX IF NOT EXISTS idx_community_applications_post
    ON community_applications(post_id);
CREATE INDEX IF NOT EXISTS idx_community_post_reports_post
    ON community_post_reports(post_id);
CREATE INDEX IF NOT EXISTS idx_match_evaluations_booking
    ON match_evaluations(booking_id);
