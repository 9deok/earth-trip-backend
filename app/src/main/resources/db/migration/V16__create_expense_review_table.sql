CREATE TABLE expense_review_days
(
    trip_id      VARCHAR(36)   NOT NULL,
    local_date   DATE          NOT NULL,
    completed_by VARCHAR(36)   NOT NULL,
    note         VARCHAR(1000) NULL,
    completed_at DATETIME(6)   NOT NULL,
    version      BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_expense_review_days PRIMARY KEY (trip_id, local_date),
    CONSTRAINT fk_expense_review_days_trip FOREIGN KEY (trip_id) REFERENCES trips (id),
    INDEX ix_expense_review_days_completed (trip_id, completed_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
