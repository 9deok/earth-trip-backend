CREATE TABLE notifications
(
    id          VARCHAR(36)  NOT NULL,
    user_id     VARCHAR(36)  NOT NULL,
    trip_id     VARCHAR(36)  NULL,
    type        VARCHAR(50)  NOT NULL,
    title       VARCHAR(160) NOT NULL,
    body        VARCHAR(500) NOT NULL,
    deep_link   VARCHAR(500) NULL,
    metadata    JSON         NOT NULL,
    created_at  DATETIME(6)  NOT NULL,
    read_at     DATETIME(6)  NULL,
    hidden_at   DATETIME(6)  NULL,
    version     BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_notifications PRIMARY KEY (id),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX ix_notifications_user_created (user_id, hidden_at, created_at),
    INDEX ix_notifications_user_unread (user_id, read_at, hidden_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE notification_preferences
(
    user_id          VARCHAR(36) NOT NULL,
    mentions_enabled BOOLEAN     NOT NULL,
    schedule_enabled BOOLEAN     NOT NULL,
    expense_enabled  BOOLEAN     NOT NULL,
    invitation_enabled BOOLEAN   NOT NULL,
    push_enabled     BOOLEAN     NOT NULL,
    email_enabled    BOOLEAN     NOT NULL,
    quiet_start      TIME        NULL,
    quiet_end        TIME        NULL,
    quiet_time_zone  VARCHAR(80) NULL,
    updated_at       DATETIME(6) NOT NULL,
    version          BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT pk_notification_preferences PRIMARY KEY (user_id),
    CONSTRAINT fk_notification_preferences_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE push_devices
(
    device_id    VARCHAR(100) NOT NULL,
    user_id      VARCHAR(36)  NOT NULL,
    platform     VARCHAR(20)  NOT NULL,
    token_hash   VARCHAR(64)  NOT NULL,
    token_cipher TEXT         NOT NULL,
    app_build    INT          NOT NULL,
    active       BOOLEAN      NOT NULL,
    created_at   DATETIME(6)  NOT NULL,
    updated_at   DATETIME(6)  NOT NULL,
    CONSTRAINT pk_push_devices PRIMARY KEY (device_id),
    CONSTRAINT uk_push_devices_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_push_devices_user FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX ix_push_devices_user_active (user_id, active)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
