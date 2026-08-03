CREATE TABLE trip_templates
(
    id             VARCHAR(36)  NOT NULL,
    owner_user_id  VARCHAR(36)  NOT NULL,
    source_trip_id VARCHAR(36)  NOT NULL,
    name           VARCHAR(120) NOT NULL,
    description    VARCHAR(500) NULL,
    include_scopes JSON         NOT NULL,
    snapshot       JSON         NOT NULL,
    created_at     DATETIME(6)  NOT NULL,
    updated_at     DATETIME(6)  NOT NULL,
    deleted_at     DATETIME(6)  NULL,
    version        BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_trip_templates PRIMARY KEY (id),
    CONSTRAINT fk_trip_templates_source_trip FOREIGN KEY (source_trip_id) REFERENCES trips (id),
    INDEX ix_trip_templates_owner_updated (owner_user_id, deleted_at, updated_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE trip_template_drafts
(
    request_id  VARCHAR(36) NOT NULL,
    template_id VARCHAR(36) NOT NULL,
    trip_id     VARCHAR(36) NOT NULL,
    created_by  VARCHAR(36) NOT NULL,
    created_at  DATETIME(6) NOT NULL,
    CONSTRAINT pk_trip_template_drafts PRIMARY KEY (request_id),
    CONSTRAINT uk_trip_template_drafts_trip UNIQUE (trip_id),
    CONSTRAINT fk_trip_template_drafts_template FOREIGN KEY (template_id) REFERENCES trip_templates (id),
    CONSTRAINT fk_trip_template_drafts_trip FOREIGN KEY (trip_id) REFERENCES trips (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
