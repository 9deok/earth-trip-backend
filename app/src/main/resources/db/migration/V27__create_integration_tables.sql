CREATE TABLE integration_connections
(
    id                  VARCHAR(36) NOT NULL,
    user_id             VARCHAR(36) NOT NULL,
    connection_kind     VARCHAR(30) NOT NULL,
    provider            VARCHAR(40) NOT NULL,
    status              VARCHAR(30) NOT NULL,
    scopes              JSON        NOT NULL,
    metadata            JSON        NOT NULL,
    authorization_state VARCHAR(120) NULL,
    authorization_expires_at DATETIME(6) NULL,
    last_success_at     DATETIME(6) NULL,
    error_code          VARCHAR(80) NULL,
    created_at          DATETIME(6) NOT NULL,
    updated_at          DATETIME(6) NOT NULL,
    revoked_at          DATETIME(6) NULL,
    version             BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT pk_integration_connections PRIMARY KEY (id),
    CONSTRAINT fk_integration_connections_user FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX ix_integration_connections_user_kind (user_id, connection_kind, revoked_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE integration_sync_jobs
(
    id               VARCHAR(36) NOT NULL,
    user_id          VARCHAR(36) NOT NULL,
    connection_id    VARCHAR(36) NULL,
    trip_id          VARCHAR(36) NULL,
    job_type         VARCHAR(40) NOT NULL,
    status           VARCHAR(30) NOT NULL,
    request_payload  JSON        NOT NULL,
    result_payload   JSON        NOT NULL,
    error_code       VARCHAR(80) NULL,
    attempt_count    INT         NOT NULL DEFAULT 1,
    created_at       DATETIME(6) NOT NULL,
    updated_at       DATETIME(6) NOT NULL,
    version          BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT pk_integration_sync_jobs PRIMARY KEY (id),
    CONSTRAINT fk_integration_sync_jobs_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_integration_sync_jobs_connection
        FOREIGN KEY (connection_id) REFERENCES integration_connections (id),
    CONSTRAINT fk_integration_sync_jobs_trip FOREIGN KEY (trip_id) REFERENCES trips (id),
    INDEX ix_integration_sync_jobs_user_created (user_id, created_at),
    INDEX ix_integration_sync_jobs_trip_type (trip_id, job_type, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE inbound_email_aliases
(
    id         VARCHAR(36)  NOT NULL,
    user_id    VARCHAR(36)  NOT NULL,
    alias      VARCHAR(320) NOT NULL,
    status     VARCHAR(30)  NOT NULL,
    created_at DATETIME(6)  NOT NULL,
    revoked_at DATETIME(6)  NULL,
    version    BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_inbound_email_aliases PRIMARY KEY (id),
    CONSTRAINT uk_inbound_email_alias UNIQUE (alias),
    CONSTRAINT fk_inbound_email_aliases_user FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX ix_inbound_email_aliases_user (user_id, revoked_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE trip_calendar_syncs
(
    trip_id       VARCHAR(36) NOT NULL,
    connection_id VARCHAR(36) NOT NULL,
    scope_config   JSON        NOT NULL,
    status         VARCHAR(30) NOT NULL,
    created_by     VARCHAR(36) NOT NULL,
    created_at     DATETIME(6) NOT NULL,
    updated_at     DATETIME(6) NOT NULL,
    version        BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT pk_trip_calendar_syncs PRIMARY KEY (trip_id),
    CONSTRAINT fk_trip_calendar_syncs_trip FOREIGN KEY (trip_id) REFERENCES trips (id),
    CONSTRAINT fk_trip_calendar_syncs_connection
        FOREIGN KEY (connection_id) REFERENCES integration_connections (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
