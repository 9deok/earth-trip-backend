CREATE TABLE trips
(
    id         VARCHAR(36)  NOT NULL,
    title      VARCHAR(100) NOT NULL,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    version    BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_trips PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
