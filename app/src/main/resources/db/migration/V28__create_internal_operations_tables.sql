CREATE TABLE operational_jobs
(
    id              VARCHAR(36)  NOT NULL,
    job_type        VARCHAR(50)  NOT NULL,
    source_event_id VARCHAR(160) NULL,
    status          VARCHAR(30)  NOT NULL,
    payload         JSON         NOT NULL,
    attempt_count   INT          NOT NULL DEFAULT 1,
    available_at    DATETIME(6)  NOT NULL,
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    completed_at    DATETIME(6)  NULL,
    error_code      VARCHAR(80)  NULL,
    error_message   VARCHAR(500) NULL,
    version         BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_operational_jobs PRIMARY KEY (id),
    INDEX ix_operational_jobs_status_available (status, available_at),
    INDEX ix_operational_jobs_type_created (job_type, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE webhook_receipts
(
    id             VARCHAR(36)  NOT NULL,
    provider       VARCHAR(40)  NOT NULL,
    source_event_id VARCHAR(160) NOT NULL,
    payload_digest CHAR(64)     NOT NULL,
    job_id         VARCHAR(36)  NOT NULL,
    received_at    DATETIME(6)  NOT NULL,
    CONSTRAINT pk_webhook_receipts PRIMARY KEY (id),
    CONSTRAINT uk_webhook_receipts_provider_event UNIQUE (provider, source_event_id),
    CONSTRAINT fk_webhook_receipts_job FOREIGN KEY (job_id) REFERENCES operational_jobs (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE dead_letter_events
(
    id            VARCHAR(36)  NOT NULL,
    job_id        VARCHAR(36)  NOT NULL,
    event_type    VARCHAR(50)  NOT NULL,
    payload       JSON         NOT NULL,
    error_code    VARCHAR(80)  NOT NULL,
    error_message VARCHAR(500) NOT NULL,
    status        VARCHAR(30)  NOT NULL,
    created_at    DATETIME(6)  NOT NULL,
    replayed_at   DATETIME(6)  NULL,
    version       BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_dead_letter_events PRIMARY KEY (id),
    CONSTRAINT fk_dead_letter_events_job FOREIGN KEY (job_id) REFERENCES operational_jobs (id),
    INDEX ix_dead_letter_events_status_created (status, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE admin_audit_events
(
    sequence_id BIGINT       NOT NULL AUTO_INCREMENT,
    event_id    VARCHAR(36)  NOT NULL,
    actor_type  VARCHAR(30)  NOT NULL,
    actor_id    VARCHAR(160) NULL,
    action      VARCHAR(80)  NOT NULL,
    target_type VARCHAR(50)  NOT NULL,
    target_id   VARCHAR(160) NULL,
    outcome     VARCHAR(30)  NOT NULL,
    metadata    JSON         NOT NULL,
    occurred_at DATETIME(6)  NOT NULL,
    CONSTRAINT pk_admin_audit_events PRIMARY KEY (sequence_id),
    CONSTRAINT uk_admin_audit_events_event UNIQUE (event_id),
    INDEX ix_admin_audit_events_occurred (occurred_at),
    INDEX ix_admin_audit_events_target (target_type, target_id, occurred_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
