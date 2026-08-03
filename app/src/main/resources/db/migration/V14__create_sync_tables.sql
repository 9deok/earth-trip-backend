CREATE TABLE sync_conflicts
(
    id               VARCHAR(36) NOT NULL,
    operation_id     VARCHAR(36) NOT NULL,
    trip_id          VARCHAR(36) NOT NULL,
    actor_id         VARCHAR(36) NOT NULL,
    action           VARCHAR(30) NOT NULL,
    resource_type    VARCHAR(50) NOT NULL,
    resource_id      VARCHAR(36) NOT NULL,
    device_command   JSON        NOT NULL,
    server_snapshot  JSON        NULL,
    mergeable_fields JSON        NOT NULL,
    status           VARCHAR(30) NOT NULL,
    resolution       VARCHAR(30) NULL,
    created_at       DATETIME(6) NOT NULL,
    resolved_at      DATETIME(6) NULL,
    version          BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT pk_sync_conflicts PRIMARY KEY (id),
    CONSTRAINT uk_sync_conflicts_operation UNIQUE (operation_id),
    CONSTRAINT fk_sync_conflicts_trip FOREIGN KEY (trip_id) REFERENCES trips (id),
    INDEX ix_sync_conflicts_trip_status (trip_id, status, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE activity_read_cursors
(
    trip_id    VARCHAR(36) NOT NULL,
    user_id    VARCHAR(36) NOT NULL,
    sequence_id BIGINT     NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_activity_read_cursors PRIMARY KEY (trip_id, user_id),
    CONSTRAINT fk_activity_read_cursors_trip FOREIGN KEY (trip_id) REFERENCES trips (id),
    CONSTRAINT fk_activity_read_cursors_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
