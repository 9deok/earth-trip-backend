CREATE TABLE wallet_records
(
    id            VARCHAR(36) NOT NULL,
    trip_id       VARCHAR(36) NOT NULL,
    record_type   VARCHAR(40) NOT NULL,
    parent_id     VARCHAR(36) NULL,
    payload       JSON        NOT NULL,
    status        VARCHAR(40) NOT NULL,
    visibility    VARCHAR(20) NOT NULL,
    sort_order    INT         NOT NULL DEFAULT 0,
    created_by    VARCHAR(36) NOT NULL,
    updated_by    VARCHAR(36) NOT NULL,
    created_at    DATETIME(6) NOT NULL,
    updated_at    DATETIME(6) NOT NULL,
    deleted_at    DATETIME(6) NULL,
    version       BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT pk_wallet_records PRIMARY KEY (id),
    CONSTRAINT fk_wallet_records_trip FOREIGN KEY (trip_id) REFERENCES trips (id),
    INDEX ix_wallet_records_trip_type (trip_id, record_type, deleted_at, sort_order),
    INDEX ix_wallet_records_parent (parent_id, record_type, deleted_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE wallet_activity_events
(
    sequence_id BIGINT      NOT NULL AUTO_INCREMENT,
    event_id    VARCHAR(36) NOT NULL,
    trip_id     VARCHAR(36) NOT NULL,
    actor_id    VARCHAR(36) NOT NULL,
    action      VARCHAR(80) NOT NULL,
    record_type VARCHAR(40) NOT NULL,
    record_id   VARCHAR(36) NOT NULL,
    payload     JSON        NOT NULL,
    occurred_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_wallet_activity_events PRIMARY KEY (sequence_id),
    CONSTRAINT uk_wallet_activity_event UNIQUE (event_id),
    INDEX ix_wallet_activity_trip_sequence (trip_id, sequence_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
