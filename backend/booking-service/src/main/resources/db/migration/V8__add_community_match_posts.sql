CREATE TABLE community_posts (
    id UUID PRIMARY KEY,
    booking_id UUID NOT NULL REFERENCES bookings(id),
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
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_community_post_type CHECK (post_type IN ('LOOKING_OPPONENT', 'LOOKING_PLAYER')),
    CONSTRAINT ck_community_post_status CHECK (status IN ('OPEN', 'MATCHED', 'FULL', 'CLOSED', 'CANCELLED')),
    CONSTRAINT ck_community_players_needed CHECK (players_needed IS NULL OR players_needed > 0)
);

CREATE UNIQUE INDEX ux_community_posts_one_active_booking
    ON community_posts(booking_id)
    WHERE deleted = FALSE AND status = 'OPEN';

CREATE INDEX ix_community_posts_feed ON community_posts(status, booking_date, start_time, created_at);
CREATE INDEX ix_community_posts_owner ON community_posts(owner_id, created_at);

CREATE TABLE community_applications (
    id UUID PRIMARY KEY,
    post_id UUID NOT NULL REFERENCES community_posts(id),
    applicant_id UUID NOT NULL,
    status VARCHAR(30) NOT NULL,
    message VARCHAR(1000),
    applicant_display_name VARCHAR(255),
    applicant_avatar_url VARCHAR(1000),
    applicant_team_photo_url VARCHAR(1000),
    applicant_skill_level VARCHAR(40),
    decided_at TIMESTAMP,
    withdrawn_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_community_application_status CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'WITHDRAWN'))
);

CREATE UNIQUE INDEX ux_community_applications_one_active
    ON community_applications(post_id, applicant_id)
    WHERE deleted = FALSE AND status IN ('PENDING', 'ACCEPTED');

CREATE INDEX ix_community_applications_post ON community_applications(post_id, status, created_at);

CREATE TABLE match_evaluations (
    id UUID PRIMARY KEY,
    post_id UUID NOT NULL REFERENCES community_posts(id),
    booking_id UUID NOT NULL REFERENCES bookings(id),
    evaluator_id UUID NOT NULL,
    evaluated_user_id UUID NOT NULL,
    arrived_on_time BOOLEAN NOT NULL,
    cancelled_unexpectedly BOOLEAN NOT NULL,
    fair_play BOOLEAN NOT NULL,
    would_play_again BOOLEAN NOT NULL,
    comment VARCHAR(1000),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX ux_match_evaluations_once
    ON match_evaluations(post_id, evaluator_id, evaluated_user_id)
    WHERE deleted = FALSE;
