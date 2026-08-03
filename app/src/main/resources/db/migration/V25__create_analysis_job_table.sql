CREATE TABLE analysis_jobs
(
    id                      VARCHAR(36) NOT NULL,
    trip_id                 VARCHAR(36) NOT NULL,
    target_type             VARCHAR(40) NOT NULL,
    target_id               VARCHAR(36) NOT NULL,
    input_payload           JSON        NOT NULL,
    suggestions             JSON        NOT NULL,
    status                  VARCHAR(20) NOT NULL,
    confirmation_request_id VARCHAR(36) NULL,
    confirmed_payload       JSON        NULL,
    failure_code            VARCHAR(80) NULL,
    failure_message         VARCHAR(500) NULL,
    attempt_count           INT         NOT NULL DEFAULT 1,
    created_by              VARCHAR(36) NOT NULL,
    created_at              DATETIME(6) NOT NULL,
    updated_at              DATETIME(6) NOT NULL,
    version                 BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT pk_analysis_jobs PRIMARY KEY (id),
    CONSTRAINT fk_analysis_jobs_trip FOREIGN KEY (trip_id) REFERENCES trips (id),
    INDEX ix_analysis_jobs_target (trip_id, target_type, target_id, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
