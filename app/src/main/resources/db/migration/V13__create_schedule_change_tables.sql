CREATE TABLE schedule_changesets
(
    id              VARCHAR(36) NOT NULL,
    trip_id         VARCHAR(36) NOT NULL,
    day_id          VARCHAR(36) NOT NULL,
    requested_by    VARCHAR(36) NOT NULL,
    before_snapshot JSON        NOT NULL,
    after_snapshot  JSON        NOT NULL,
    status          VARCHAR(30) NOT NULL,
    applied_at      DATETIME(6) NOT NULL,
    reverted_at     DATETIME(6) NULL,
    version         BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT pk_schedule_changesets PRIMARY KEY (id),
    CONSTRAINT fk_schedule_changesets_trip FOREIGN KEY (trip_id) REFERENCES trips (id),
    INDEX ix_schedule_changesets_day_applied (trip_id, day_id, applied_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE schedule_diagnostic_resolutions
(
    diagnostic_id VARCHAR(36)   NOT NULL,
    trip_id       VARCHAR(36)   NOT NULL,
    day_id        VARCHAR(36)   NOT NULL,
    note          VARCHAR(1000) NULL,
    resolved_by   VARCHAR(36)   NOT NULL,
    resolved_at   DATETIME(6)   NOT NULL,
    CONSTRAINT pk_schedule_diagnostic_resolutions PRIMARY KEY (diagnostic_id),
    CONSTRAINT fk_schedule_diagnostic_resolutions_trip FOREIGN KEY (trip_id) REFERENCES trips (id),
    INDEX ix_schedule_diagnostic_resolutions_day (trip_id, day_id, resolved_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
