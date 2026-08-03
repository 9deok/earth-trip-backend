CREATE TABLE expenses
(
    id                  VARCHAR(36)  NOT NULL,
    trip_id             VARCHAR(36)  NOT NULL,
    title               VARCHAR(200) NOT NULL,
    category_code       VARCHAR(80)  NOT NULL,
    amount_minor        BIGINT       NOT NULL,
    currency            CHAR(3)      NOT NULL,
    occurred_at         DATETIME(6)  NOT NULL,
    payer_contributions JSON         NOT NULL,
    participant_shares  JSON         NOT NULL,
    visibility          VARCHAR(20)  NOT NULL,
    status              VARCHAR(30)  NOT NULL,
    note                TEXT         NULL,
    created_by          VARCHAR(36)  NOT NULL,
    updated_by          VARCHAR(36)  NOT NULL,
    created_at          DATETIME(6)  NOT NULL,
    updated_at          DATETIME(6)  NOT NULL,
    deleted_at          DATETIME(6)  NULL,
    version             BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_expenses PRIMARY KEY (id),
    CONSTRAINT fk_expenses_trip FOREIGN KEY (trip_id) REFERENCES trips (id),
    INDEX ix_expenses_trip_occurred (trip_id, occurred_at, deleted_at),
    INDEX ix_expenses_trip_category (trip_id, category_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE expense_adjustments
(
    id            VARCHAR(36) NOT NULL,
    trip_id       VARCHAR(36) NOT NULL,
    expense_id    VARCHAR(36) NOT NULL,
    kind          VARCHAR(30) NOT NULL,
    amount_minor  BIGINT      NOT NULL,
    currency      CHAR(3)     NOT NULL,
    participant_id VARCHAR(36) NULL,
    payload       JSON        NOT NULL,
    created_by    VARCHAR(36) NOT NULL,
    created_at    DATETIME(6) NOT NULL,
    CONSTRAINT pk_expense_adjustments PRIMARY KEY (id),
    CONSTRAINT fk_expense_adjustment_expense FOREIGN KEY (expense_id) REFERENCES expenses (id),
    INDEX ix_expense_adjustments_expense (expense_id, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE cash_movements
(
    id           VARCHAR(36) NOT NULL,
    trip_id      VARCHAR(36) NOT NULL,
    movement_type VARCHAR(30) NOT NULL,
    amount_minor BIGINT      NOT NULL,
    currency     CHAR(3)     NOT NULL,
    payload      JSON        NOT NULL,
    status       VARCHAR(30) NOT NULL,
    occurred_at  DATETIME(6) NOT NULL,
    created_by   VARCHAR(36) NOT NULL,
    created_at   DATETIME(6) NOT NULL,
    updated_at   DATETIME(6) NOT NULL,
    deleted_at   DATETIME(6) NULL,
    version      BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT pk_cash_movements PRIMARY KEY (id),
    CONSTRAINT fk_cash_movements_trip FOREIGN KEY (trip_id) REFERENCES trips (id),
    INDEX ix_cash_movements_trip_currency (trip_id, currency, occurred_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE exchange_rate_snapshots
(
    id             VARCHAR(36) NOT NULL,
    trip_id        VARCHAR(36) NOT NULL,
    base_currency  CHAR(3)     NOT NULL,
    quote_currency CHAR(3)     NOT NULL,
    rate_value     DECIMAL(24, 12) NOT NULL,
    source         VARCHAR(80) NOT NULL,
    observed_at    DATETIME(6) NOT NULL,
    created_by     VARCHAR(36) NOT NULL,
    created_at     DATETIME(6) NOT NULL,
    CONSTRAINT pk_exchange_rate_snapshots PRIMARY KEY (id),
    CONSTRAINT fk_exchange_rates_trip FOREIGN KEY (trip_id) REFERENCES trips (id),
    INDEX ix_exchange_rates_pair_observed (trip_id, base_currency, quote_currency, observed_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE settlements
(
    id            VARCHAR(36) NOT NULL,
    trip_id       VARCHAR(36) NOT NULL,
    base_currency CHAR(3)     NOT NULL,
    status        VARCHAR(30) NOT NULL,
    snapshot_json JSON        NOT NULL,
    created_by    VARCHAR(36) NOT NULL,
    created_at    DATETIME(6) NOT NULL,
    updated_at    DATETIME(6) NOT NULL,
    closed_at     DATETIME(6) NULL,
    version       BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT pk_settlements PRIMARY KEY (id),
    CONSTRAINT fk_settlements_trip FOREIGN KEY (trip_id) REFERENCES trips (id),
    INDEX ix_settlements_trip_created (trip_id, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE settlement_payments
(
    id            VARCHAR(36) NOT NULL,
    settlement_id VARCHAR(36) NOT NULL,
    from_user_id  VARCHAR(36) NOT NULL,
    to_user_id    VARCHAR(36) NOT NULL,
    amount_minor  BIGINT      NOT NULL,
    currency      CHAR(3)     NOT NULL,
    paid_at       DATETIME(6) NULL,
    note          VARCHAR(500) NULL,
    created_at    DATETIME(6) NOT NULL,
    updated_at    DATETIME(6) NOT NULL,
    version       BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT pk_settlement_payments PRIMARY KEY (id),
    CONSTRAINT fk_settlement_payments_settlement FOREIGN KEY (settlement_id) REFERENCES settlements (id),
    INDEX ix_settlement_payments_settlement (settlement_id, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
