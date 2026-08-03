CREATE TABLE statement_imports
(
    id         VARCHAR(36) NOT NULL,
    trip_id    VARCHAR(36) NOT NULL,
    source     VARCHAR(80) NOT NULL,
    status     VARCHAR(30) NOT NULL,
    created_by VARCHAR(36) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version    BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT pk_statement_imports PRIMARY KEY (id),
    CONSTRAINT fk_statement_imports_trip FOREIGN KEY (trip_id) REFERENCES trips (id),
    INDEX ix_statement_imports_trip_created (trip_id, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE statement_import_candidates
(
    id            VARCHAR(36)  NOT NULL,
    import_id     VARCHAR(36)  NOT NULL,
    trip_id       VARCHAR(36)  NOT NULL,
    title         VARCHAR(200) NOT NULL,
    amount_minor  BIGINT       NOT NULL,
    currency      CHAR(3)      NOT NULL,
    occurred_at   DATETIME(6)  NOT NULL,
    payer_user_id VARCHAR(36)  NOT NULL,
    payload       JSON         NOT NULL,
    status        VARCHAR(30)  NOT NULL,
    expense_id    VARCHAR(36)  NULL,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    version       BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_statement_import_candidates PRIMARY KEY (id),
    CONSTRAINT fk_statement_candidates_import FOREIGN KEY (import_id) REFERENCES statement_imports (id),
    CONSTRAINT fk_statement_candidates_trip FOREIGN KEY (trip_id) REFERENCES trips (id),
    CONSTRAINT fk_statement_candidates_expense FOREIGN KEY (expense_id) REFERENCES expenses (id),
    INDEX ix_statement_candidates_import_status (import_id, status, occurred_at),
    INDEX ix_statement_candidates_expense (expense_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
