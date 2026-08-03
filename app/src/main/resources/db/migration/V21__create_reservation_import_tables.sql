CREATE TABLE reservation_import_jobs
(
    id              VARCHAR(36) NOT NULL,
    trip_id         VARCHAR(36) NOT NULL,
    source_type     VARCHAR(30) NOT NULL,
    source_payload  JSON        NOT NULL,
    status          VARCHAR(30) NOT NULL,
    failure_code    VARCHAR(80) NULL,
    failure_message VARCHAR(500) NULL,
    attempt_count   INT         NOT NULL DEFAULT 0,
    created_by      VARCHAR(36) NOT NULL,
    created_at      DATETIME(6) NOT NULL,
    updated_at      DATETIME(6) NOT NULL,
    version         BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT pk_reservation_import_jobs PRIMARY KEY (id),
    CONSTRAINT fk_reservation_import_jobs_trip FOREIGN KEY (trip_id) REFERENCES trips (id),
    INDEX ix_reservation_import_jobs_trip_created (trip_id, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE reservation_import_candidates
(
    id             VARCHAR(36)  NOT NULL,
    job_id         VARCHAR(36)  NOT NULL,
    trip_id        VARCHAR(36)  NOT NULL,
    title          VARCHAR(200) NOT NULL,
    candidate_type VARCHAR(40)  NOT NULL,
    payload        JSON         NOT NULL,
    confidence     DECIMAL(5,4) NULL,
    status         VARCHAR(30)  NOT NULL,
    reservation_id VARCHAR(36)  NULL,
    dismissal_reason VARCHAR(500) NULL,
    created_at     DATETIME(6)  NOT NULL,
    updated_at     DATETIME(6)  NOT NULL,
    version        BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_reservation_import_candidates PRIMARY KEY (id),
    CONSTRAINT fk_reservation_import_candidates_job
        FOREIGN KEY (job_id) REFERENCES reservation_import_jobs (id),
    CONSTRAINT fk_reservation_import_candidates_trip
        FOREIGN KEY (trip_id) REFERENCES trips (id),
    CONSTRAINT fk_reservation_import_candidates_reservation
        FOREIGN KEY (reservation_id) REFERENCES wallet_records (id),
    INDEX ix_reservation_import_candidates_job_status (job_id, status, created_at),
    INDEX ix_reservation_import_candidates_reservation (reservation_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
