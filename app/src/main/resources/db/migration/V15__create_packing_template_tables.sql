CREATE TABLE packing_templates
(
    id         VARCHAR(36)  NOT NULL,
    user_id    VARCHAR(36)  NOT NULL,
    name       VARCHAR(120) NOT NULL,
    visibility VARCHAR(20)  NOT NULL,
    items      JSON         NOT NULL,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    deleted_at DATETIME(6)  NULL,
    version    BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_packing_templates PRIMARY KEY (id),
    CONSTRAINT fk_packing_templates_user FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX ix_packing_templates_owner (user_id, deleted_at, updated_at),
    INDEX ix_packing_templates_visibility (visibility, deleted_at, updated_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE packing_template_applications
(
    id          VARCHAR(36) NOT NULL,
    trip_id     VARCHAR(36) NOT NULL,
    template_id VARCHAR(36) NOT NULL,
    applied_by  VARCHAR(36) NOT NULL,
    applied_at  DATETIME(6) NOT NULL,
    item_ids    JSON        NOT NULL,
    CONSTRAINT pk_packing_template_applications PRIMARY KEY (id),
    CONSTRAINT fk_packing_template_applications_trip FOREIGN KEY (trip_id) REFERENCES trips (id),
    CONSTRAINT fk_packing_template_applications_template FOREIGN KEY (template_id) REFERENCES packing_templates (id),
    INDEX ix_packing_template_applications_trip (trip_id, applied_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE preparation_suggestion_dismissals
(
    suggestion_id VARCHAR(36)  NOT NULL,
    trip_id       VARCHAR(36)  NOT NULL,
    user_id       VARCHAR(36)  NOT NULL,
    reason        VARCHAR(500) NULL,
    dismissed_at  DATETIME(6)  NOT NULL,
    CONSTRAINT pk_preparation_suggestion_dismissals PRIMARY KEY (suggestion_id, user_id),
    CONSTRAINT fk_preparation_suggestion_dismissals_trip FOREIGN KEY (trip_id) REFERENCES trips (id),
    CONSTRAINT fk_preparation_suggestion_dismissals_user FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX ix_preparation_suggestion_dismissals_user (trip_id, user_id, dismissed_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
