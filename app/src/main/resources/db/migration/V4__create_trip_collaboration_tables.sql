CREATE TABLE trip_members
(
    id          VARCHAR(36) NOT NULL,
    trip_id     VARCHAR(36) NOT NULL,
    user_id     VARCHAR(36) NOT NULL,
    role        VARCHAR(20) NOT NULL,
    status      VARCHAR(20) NOT NULL,
    joined_at   DATETIME(6) NOT NULL,
    updated_at  DATETIME(6) NOT NULL,
    version     BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT pk_trip_members PRIMARY KEY (id),
    CONSTRAINT uk_trip_members_trip_user UNIQUE (trip_id, user_id),
    CONSTRAINT fk_trip_members_trip FOREIGN KEY (trip_id) REFERENCES trips (id),
    CONSTRAINT fk_trip_members_user FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX ix_trip_members_user_status (user_id, status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE trip_invitations
(
    id               VARCHAR(36)  NOT NULL,
    trip_id          VARCHAR(36)  NOT NULL,
    email            VARCHAR(320) NOT NULL,
    role             VARCHAR(20)  NOT NULL,
    token_hash       VARCHAR(64)  NOT NULL,
    status           VARCHAR(30)  NOT NULL,
    invited_by       VARCHAR(36)  NOT NULL,
    invited_user_id  VARCHAR(36)  NULL,
    expires_at       DATETIME(6)  NOT NULL,
    accepted_at      DATETIME(6)  NULL,
    declined_at      DATETIME(6)  NULL,
    revoked_at       DATETIME(6)  NULL,
    last_delivered_at DATETIME(6) NULL,
    delivery_status  VARCHAR(40)  NOT NULL,
    created_at       DATETIME(6)  NOT NULL,
    updated_at       DATETIME(6)  NOT NULL,
    version          BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_trip_invitations PRIMARY KEY (id),
    CONSTRAINT uk_trip_invitations_token UNIQUE (token_hash),
    CONSTRAINT fk_trip_invitations_trip FOREIGN KEY (trip_id) REFERENCES trips (id),
    CONSTRAINT fk_trip_invitations_inviter FOREIGN KEY (invited_by) REFERENCES users (id),
    CONSTRAINT fk_trip_invitations_user FOREIGN KEY (invited_user_id) REFERENCES users (id),
    INDEX ix_trip_invitations_trip_created (trip_id, created_at),
    INDEX ix_trip_invitations_email_status (email, status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE ownership_transfers
(
    id             VARCHAR(36) NOT NULL,
    trip_id        VARCHAR(36) NOT NULL,
    from_user_id   VARCHAR(36) NOT NULL,
    to_user_id     VARCHAR(36) NOT NULL,
    confirmed_at   DATETIME(6) NOT NULL,
    created_at     DATETIME(6) NOT NULL,
    CONSTRAINT pk_ownership_transfers PRIMARY KEY (id),
    CONSTRAINT fk_ownership_transfers_trip FOREIGN KEY (trip_id) REFERENCES trips (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
