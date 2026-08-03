CREATE TABLE linked_identities
(
    id               VARCHAR(36)  NOT NULL,
    user_id          VARCHAR(36)  NOT NULL,
    provider         VARCHAR(20)  NOT NULL,
    provider_subject VARCHAR(255) NOT NULL,
    provider_email   VARCHAR(320) NULL,
    created_at       DATETIME(6)  NOT NULL,
    last_used_at     DATETIME(6)  NOT NULL,
    version          BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_linked_identities PRIMARY KEY (id),
    CONSTRAINT uk_linked_identity_provider_subject UNIQUE (provider, provider_subject),
    CONSTRAINT fk_linked_identities_user FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX ix_linked_identities_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE email_change_requests
(
    id           VARCHAR(36)  NOT NULL,
    user_id      VARCHAR(36)  NOT NULL,
    new_email    VARCHAR(320) NOT NULL,
    token_hash   VARCHAR(64)  NOT NULL,
    status       VARCHAR(20)  NOT NULL,
    expires_at   DATETIME(6)  NOT NULL,
    created_at   DATETIME(6)  NOT NULL,
    confirmed_at DATETIME(6)  NULL,
    CONSTRAINT pk_email_change_requests PRIMARY KEY (id),
    CONSTRAINT uk_email_change_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_email_change_requests_user FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX ix_email_change_requests_user_status (user_id, status, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
