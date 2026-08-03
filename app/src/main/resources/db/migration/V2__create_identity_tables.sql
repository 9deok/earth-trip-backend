CREATE TABLE users
(
    id                VARCHAR(36)  NOT NULL,
    email             VARCHAR(320) NOT NULL,
    password_hash     VARCHAR(255) NOT NULL,
    display_name      VARCHAR(80)  NOT NULL,
    status            VARCHAR(30)  NOT NULL,
    email_verified_at DATETIME(6)  NULL,
    created_at        DATETIME(6)  NOT NULL,
    updated_at        DATETIME(6)  NOT NULL,
    version           BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE auth_tokens
(
    id          VARCHAR(36)  NOT NULL,
    user_id     VARCHAR(36)  NOT NULL,
    purpose     VARCHAR(40)  NOT NULL,
    token_hash  VARCHAR(64)  NOT NULL,
    expires_at  DATETIME(6)  NOT NULL,
    consumed_at DATETIME(6)  NULL,
    created_at  DATETIME(6)  NOT NULL,
    CONSTRAINT pk_auth_tokens PRIMARY KEY (id),
    CONSTRAINT uk_auth_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_auth_tokens_user FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX ix_auth_tokens_user_purpose (user_id, purpose),
    INDEX ix_auth_tokens_expires_at (expires_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE auth_sessions
(
    id                 VARCHAR(36)  NOT NULL,
    user_id            VARCHAR(36)  NOT NULL,
    access_token_hash  VARCHAR(64)  NOT NULL,
    refresh_token_hash VARCHAR(64)  NOT NULL,
    device_name        VARCHAR(120) NOT NULL,
    access_expires_at  DATETIME(6)  NOT NULL,
    refresh_expires_at DATETIME(6)  NOT NULL,
    last_used_at       DATETIME(6)  NOT NULL,
    revoked_at         DATETIME(6)  NULL,
    created_at         DATETIME(6)  NOT NULL,
    version            BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_auth_sessions PRIMARY KEY (id),
    CONSTRAINT uk_auth_sessions_access_hash UNIQUE (access_token_hash),
    CONSTRAINT uk_auth_sessions_refresh_hash UNIQUE (refresh_token_hash),
    CONSTRAINT fk_auth_sessions_user FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX ix_auth_sessions_user (user_id),
    INDEX ix_auth_sessions_refresh_expiry (refresh_expires_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE user_preferences
(
    user_id                VARCHAR(36) NOT NULL,
    locale                 VARCHAR(20) NOT NULL,
    default_currency       CHAR(3)     NOT NULL,
    time_zone              VARCHAR(80) NOT NULL,
    share_ticket_names     BOOLEAN     NOT NULL DEFAULT FALSE,
    share_personal_expense BOOLEAN     NOT NULL DEFAULT FALSE,
    optional_analytics     BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at             DATETIME(6) NOT NULL,
    updated_at             DATETIME(6) NOT NULL,
    version                BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT pk_user_preferences PRIMARY KEY (user_id),
    CONSTRAINT fk_user_preferences_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE policy_documents
(
    id           VARCHAR(80)  NOT NULL,
    policy_type  VARCHAR(40)  NOT NULL,
    version_name VARCHAR(40)  NOT NULL,
    required     BOOLEAN      NOT NULL,
    title        VARCHAR(160) NOT NULL,
    summary      TEXT         NOT NULL,
    content_url  VARCHAR(500) NOT NULL,
    published_at DATETIME(6)  NOT NULL,
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_policy_documents PRIMARY KEY (id),
    CONSTRAINT uk_policy_type_version UNIQUE (policy_type, version_name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE policy_consents
(
    user_id      VARCHAR(36) NOT NULL,
    policy_id    VARCHAR(80) NOT NULL,
    decision     VARCHAR(20) NOT NULL,
    decided_at   DATETIME(6) NOT NULL,
    source       VARCHAR(30) NOT NULL,
    version      BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT pk_policy_consents PRIMARY KEY (user_id, policy_id),
    CONSTRAINT fk_policy_consents_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_policy_consents_policy FOREIGN KEY (policy_id) REFERENCES policy_documents (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE account_deletion_requests
(
    id                    VARCHAR(36) NOT NULL,
    user_id               VARCHAR(36) NOT NULL,
    requested_at          DATETIME(6) NOT NULL,
    scheduled_deletion_at DATETIME(6) NOT NULL,
    cancelled_at          DATETIME(6) NULL,
    completed_at          DATETIME(6) NULL,
    version               BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT pk_account_deletion_requests PRIMARY KEY (id),
    CONSTRAINT fk_account_deletion_user FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX ix_account_deletion_user (user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE data_export_jobs
(
    id           VARCHAR(36)  NOT NULL,
    user_id      VARCHAR(36)  NOT NULL,
    status       VARCHAR(30)  NOT NULL,
    format       VARCHAR(30)  NOT NULL,
    file_id      VARCHAR(36)  NULL,
    error_code   VARCHAR(80)  NULL,
    created_at   DATETIME(6)  NOT NULL,
    completed_at DATETIME(6)  NULL,
    expires_at   DATETIME(6)  NULL,
    CONSTRAINT pk_data_export_jobs PRIMARY KEY (id),
    CONSTRAINT fk_data_export_jobs_user FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX ix_data_export_jobs_user_created (user_id, created_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE favorite_companions
(
    id           VARCHAR(36)  NOT NULL,
    user_id      VARCHAR(36)  NOT NULL,
    companion_id VARCHAR(36)  NULL,
    display_name VARCHAR(80)  NOT NULL,
    email        VARCHAR(320) NULL,
    created_at   DATETIME(6)  NOT NULL,
    CONSTRAINT pk_favorite_companions PRIMARY KEY (id),
    CONSTRAINT fk_favorite_companions_user FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX ix_favorite_companions_user (user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE support_requests
(
    id          VARCHAR(36)  NOT NULL,
    user_id     VARCHAR(36)  NULL,
    category    VARCHAR(40)  NOT NULL,
    description TEXT         NOT NULL,
    trace_id    VARCHAR(100) NULL,
    diagnostics TEXT         NULL,
    status      VARCHAR(30)  NOT NULL,
    created_at  DATETIME(6)  NOT NULL,
    CONSTRAINT pk_support_requests PRIMARY KEY (id),
    CONSTRAINT fk_support_requests_user FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX ix_support_requests_user_created (user_id, created_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

INSERT INTO policy_documents
    (id, policy_type, version_name, required, title, summary, content_url, published_at, active)
VALUES
    ('terms-2026-08', 'TERMS', '2026-08', TRUE, '서비스 이용약관',
     'Earth Trip 계정과 공동 여행 공간 이용 조건',
     'https://earthtrip.app/legal/terms/2026-08', UTC_TIMESTAMP(6), TRUE),
    ('privacy-2026-08', 'PRIVACY', '2026-08', TRUE, '개인정보 처리방침',
     '계정, 여행, 예약자료, 지출정보의 처리와 삭제',
     'https://earthtrip.app/legal/privacy/2026-08', UTC_TIMESTAMP(6), TRUE),
    ('analytics-2026-08', 'ANALYTICS', '2026-08', FALSE, '선택적 사용 분석',
     '앱 개선을 위한 선택적 진단·사용 정보 수집',
     'https://earthtrip.app/legal/analytics/2026-08', UTC_TIMESTAMP(6), TRUE);
