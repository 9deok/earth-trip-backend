CREATE TABLE settlement_supplements
(
    id                     VARCHAR(36) NOT NULL,
    original_settlement_id VARCHAR(36) NOT NULL,
    supplement_settlement_id VARCHAR(36) NOT NULL,
    created_by             VARCHAR(36) NOT NULL,
    created_at             DATETIME(6) NOT NULL,
    CONSTRAINT pk_settlement_supplements PRIMARY KEY (id),
    CONSTRAINT uk_settlement_supplement_child UNIQUE (supplement_settlement_id),
    CONSTRAINT fk_settlement_supplement_original FOREIGN KEY (original_settlement_id) REFERENCES settlements (id),
    CONSTRAINT fk_settlement_supplement_child FOREIGN KEY (supplement_settlement_id) REFERENCES settlements (id),
    INDEX ix_settlement_supplements_original (original_settlement_id, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
