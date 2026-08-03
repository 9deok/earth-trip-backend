CREATE TABLE trip_export_jobs
(
    id              VARCHAR(36)  NOT NULL,
    trip_id         VARCHAR(36)  NOT NULL,
    format          VARCHAR(10)  NOT NULL,
    scopes          JSON         NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    file_name       VARCHAR(255) NULL,
    mime_type       VARCHAR(100) NULL,
    artifact        LONGBLOB     NULL,
    checksum_sha256 CHAR(64)     NULL,
    failure_code    VARCHAR(80)  NULL,
    failure_message VARCHAR(500) NULL,
    attempt_count   INT          NOT NULL DEFAULT 1,
    created_by      VARCHAR(36)  NOT NULL,
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    version         BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_trip_export_jobs PRIMARY KEY (id),
    CONSTRAINT fk_trip_export_jobs_trip FOREIGN KEY (trip_id) REFERENCES trips (id),
    INDEX ix_trip_export_jobs_trip_created (trip_id, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
