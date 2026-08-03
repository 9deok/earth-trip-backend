CREATE TABLE trip_share_links
(
    id                 VARCHAR(36)  NOT NULL,
    trip_id            VARCHAR(36)  NOT NULL,
    token_hash         VARCHAR(64)  NOT NULL,
    name               VARCHAR(120) NOT NULL,
    scopes_json        JSON         NOT NULL,
    password_hash      VARCHAR(500) NULL,
    projection_user_id VARCHAR(36)  NOT NULL,
    expires_at         DATETIME(6)  NULL,
    status             VARCHAR(30)  NOT NULL,
    created_by         VARCHAR(36)  NOT NULL,
    created_at         DATETIME(6)  NOT NULL,
    updated_at         DATETIME(6)  NOT NULL,
    revoked_at         DATETIME(6)  NULL,
    version            BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_trip_share_links PRIMARY KEY (id),
    CONSTRAINT uk_trip_share_links_token UNIQUE (token_hash),
    CONSTRAINT fk_trip_share_links_trip FOREIGN KEY (trip_id) REFERENCES trips (id),
    INDEX ix_trip_share_links_trip (trip_id, status, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE trip_share_password_sessions
(
    token_hash VARCHAR(64) NOT NULL,
    share_id   VARCHAR(36) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_trip_share_password_sessions PRIMARY KEY (token_hash),
    CONSTRAINT fk_trip_share_sessions_share FOREIGN KEY (share_id) REFERENCES trip_share_links (id),
    INDEX ix_trip_share_sessions_expiry (share_id, expires_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE trip_share_access_events
(
    sequence_id BIGINT       NOT NULL AUTO_INCREMENT,
    event_id    VARCHAR(36)  NOT NULL,
    share_id    VARCHAR(36)  NOT NULL,
    success     BOOLEAN      NOT NULL,
    reason      VARCHAR(50)  NOT NULL,
    occurred_at DATETIME(6)  NOT NULL,
    CONSTRAINT pk_trip_share_access_events PRIMARY KEY (sequence_id),
    CONSTRAINT uk_trip_share_access_event UNIQUE (event_id),
    CONSTRAINT fk_trip_share_access_share FOREIGN KEY (share_id) REFERENCES trip_share_links (id),
    INDEX ix_trip_share_access_share (share_id, occurred_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
