CREATE TABLE expense_categories
(
    id          VARCHAR(36)  NOT NULL,
    trip_id     VARCHAR(36)  NOT NULL,
    code        VARCHAR(80)  NOT NULL,
    name        VARCHAR(100) NOT NULL,
    color       VARCHAR(20)  NOT NULL,
    sort_order  INT          NOT NULL,
    created_by  VARCHAR(36)  NOT NULL,
    updated_by  VARCHAR(36)  NOT NULL,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    deleted_at  DATETIME(6)  NULL,
    version     BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_expense_categories PRIMARY KEY (id),
    CONSTRAINT uk_expense_categories_trip_code UNIQUE (trip_id, code),
    CONSTRAINT fk_expense_categories_trip FOREIGN KEY (trip_id) REFERENCES trips (id),
    INDEX ix_expense_categories_trip_order (trip_id, sort_order, deleted_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
