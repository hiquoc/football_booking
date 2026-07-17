ALTER TABLE community_posts
    ADD COLUMN IF NOT EXISTS field_owner_id UUID,
    ADD COLUMN IF NOT EXISTS hidden_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS hidden_reason VARCHAR(500);

UPDATE community_posts p
SET field_owner_id = b.owner_id
FROM bookings b
WHERE p.booking_id = b.id
  AND p.field_owner_id IS NULL;

ALTER TABLE community_posts
    DROP CONSTRAINT IF EXISTS ck_community_post_status;

ALTER TABLE community_posts
    ADD CONSTRAINT ck_community_post_status
    CHECK (status IN ('OPEN', 'MATCHED', 'FULL', 'CLOSED', 'CANCELLED', 'HIDDEN'));

CREATE TABLE community_post_reports (
    id UUID PRIMARY KEY,
    post_id UUID NOT NULL REFERENCES community_posts(id),
    reporter_id UUID NOT NULL,
    reason VARCHAR(50) NOT NULL,
    description VARCHAR(1000),
    status VARCHAR(30) NOT NULL,
    reviewed_by UUID,
    reviewed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_community_report_reason CHECK (reason IN ('SPAM', 'INAPPROPRIATE_CONTENT', 'HARASSMENT', 'FAKE_INFORMATION', 'SCAM', 'OTHER')),
    CONSTRAINT ck_community_report_status CHECK (status IN ('PENDING', 'REVIEWED'))
);

CREATE UNIQUE INDEX ux_community_post_reports_once
    ON community_post_reports(post_id, reporter_id)
    WHERE deleted = FALSE;

CREATE INDEX ix_community_post_reports_status ON community_post_reports(status, created_at);

CREATE TABLE community_user_violations (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    reason VARCHAR(500) NOT NULL,
    action VARCHAR(50) NOT NULL,
    expire_at TIMESTAMP,
    status VARCHAR(30) NOT NULL,
    source_post_id UUID REFERENCES community_posts(id),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_community_violation_action CHECK (action IN ('ISSUE_WARNING', 'TEMPORARY_POSTING_BAN', 'PERMANENT_POSTING_BAN')),
    CONSTRAINT ck_community_violation_status CHECK (status IN ('ACTIVE', 'EXPIRED', 'PERMANENT'))
);

CREATE INDEX ix_community_user_violations_active ON community_user_violations(user_id, status, expire_at);

CREATE TABLE community_moderation_history (
    id UUID PRIMARY KEY,
    target_user_id UUID NOT NULL,
    target_post_id UUID REFERENCES community_posts(id),
    moderator_id UUID NOT NULL,
    action VARCHAR(60) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    note VARCHAR(1000),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_community_moderation_action CHECK (action IN ('NO_ACTION', 'HIDE_POST', 'RESTORE_POST', 'ISSUE_WARNING', 'TEMPORARY_POSTING_BAN', 'PERMANENT_POSTING_BAN', 'OWNER_HIDE_POST', 'AUTO_HIDE_POSTS_AFTER_BAN', 'AUTO_REJECT_APPLICATIONS_AFTER_MODERATION'))
);

CREATE INDEX ix_community_moderation_history_target ON community_moderation_history(target_user_id, created_at);
CREATE INDEX ix_community_moderation_history_post ON community_moderation_history(target_post_id, created_at);
