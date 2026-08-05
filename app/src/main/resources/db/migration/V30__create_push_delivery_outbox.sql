CREATE TABLE push_delivery_attempts
(
    id                  VARCHAR(36)  NOT NULL,
    notification_id     VARCHAR(36)  NOT NULL,
    device_id            VARCHAR(100) NOT NULL,
    status               VARCHAR(20)  NOT NULL,
    attempts             INT          NOT NULL DEFAULT 0,
    next_attempt_at      DATETIME(6)  NULL,
    last_error           VARCHAR(160) NULL,
    provider_message_id  VARCHAR(300) NULL,
    created_at           DATETIME(6)  NOT NULL,
    updated_at           DATETIME(6)  NOT NULL,
    CONSTRAINT pk_push_delivery_attempts PRIMARY KEY (id),
    CONSTRAINT uk_push_delivery_notification_device UNIQUE (notification_id, device_id),
    CONSTRAINT fk_push_delivery_notification FOREIGN KEY (notification_id)
        REFERENCES notifications (id),
    CONSTRAINT fk_push_delivery_device FOREIGN KEY (device_id)
        REFERENCES push_devices (device_id),
    INDEX ix_push_delivery_retry (status, next_attempt_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
