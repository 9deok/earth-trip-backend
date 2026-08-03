CREATE TABLE files
(
    id            VARCHAR(36)  NOT NULL,
    owner_user_id VARCHAR(36)  NOT NULL,
    file_name     VARCHAR(255) NOT NULL,
    mime_type     VARCHAR(120) NOT NULL,
    size_bytes    BIGINT       NOT NULL,
    checksum_sha256 VARCHAR(64) NOT NULL,
    storage_key   VARCHAR(500) NOT NULL,
    status        VARCHAR(30)  NOT NULL,
    created_at    DATETIME(6)  NOT NULL,
    completed_at  DATETIME(6)  NULL,
    deleted_at    DATETIME(6)  NULL,
    version       BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_files PRIMARY KEY (id),
    CONSTRAINT fk_files_owner FOREIGN KEY (owner_user_id) REFERENCES users (id),
    INDEX ix_files_owner_created (owner_user_id, created_at),
    INDEX ix_files_checksum (checksum_sha256, size_bytes)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE file_upload_sessions
(
    id          VARCHAR(36)  NOT NULL,
    file_id     VARCHAR(36)  NOT NULL,
    status      VARCHAR(30)  NOT NULL,
    expires_at  DATETIME(6)  NOT NULL,
    created_at  DATETIME(6)  NOT NULL,
    aborted_at  DATETIME(6)  NULL,
    CONSTRAINT pk_file_upload_sessions PRIMARY KEY (id),
    CONSTRAINT fk_upload_sessions_file FOREIGN KEY (file_id) REFERENCES files (id),
    INDEX ix_upload_sessions_file (file_id, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE file_links
(
    id            VARCHAR(36) NOT NULL,
    file_id       VARCHAR(36) NOT NULL,
    trip_id       VARCHAR(36) NOT NULL,
    resource_type VARCHAR(50) NOT NULL,
    resource_id   VARCHAR(36) NOT NULL,
    visibility    VARCHAR(20) NOT NULL,
    linked_by     VARCHAR(36) NOT NULL,
    linked_at     DATETIME(6) NOT NULL,
    CONSTRAINT pk_file_links PRIMARY KEY (id),
    CONSTRAINT uk_file_link_target UNIQUE (file_id, resource_type, resource_id),
    CONSTRAINT fk_file_links_file FOREIGN KEY (file_id) REFERENCES files (id),
    CONSTRAINT fk_file_links_trip FOREIGN KEY (trip_id) REFERENCES trips (id),
    INDEX ix_file_links_file (file_id),
    INDEX ix_file_links_trip_resource (trip_id, resource_type, resource_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
