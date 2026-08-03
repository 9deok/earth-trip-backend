ALTER TABLE trips
    ADD COLUMN owner_user_id VARCHAR(36) NULL AFTER id,
    ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT 'DRAFT' AFTER title,
    ADD COLUMN start_date DATE NULL AFTER status,
    ADD COLUMN end_date DATE NULL AFTER start_date,
    ADD COLUMN time_zone VARCHAR(80) NOT NULL DEFAULT 'Asia/Seoul' AFTER end_date,
    ADD COLUMN default_currency CHAR(3) NOT NULL DEFAULT 'KRW' AFTER time_zone,
    ADD COLUMN planning_mode VARCHAR(30) NOT NULL DEFAULT 'EXACT' AFTER default_currency,
    ADD COLUMN pace VARCHAR(30) NOT NULL DEFAULT 'BALANCED' AFTER planning_mode,
    ADD COLUMN deleted_at DATETIME(6) NULL AFTER pace,
    ADD COLUMN scheduled_deletion_at DATETIME(6) NULL AFTER deleted_at,
    ADD INDEX ix_trips_owner_updated (owner_user_id, updated_at),
    ADD INDEX ix_trips_status_dates (status, start_date, end_date);

CREATE TABLE trip_segments
(
    id                  VARCHAR(36)  NOT NULL,
    trip_id             VARCHAR(36)  NOT NULL,
    segment_type        VARCHAR(30)  NOT NULL,
    city_name           VARCHAR(160) NULL,
    country_code        CHAR(2)      NULL,
    place_id            VARCHAR(255) NULL,
    latitude            DECIMAL(10, 7) NULL,
    longitude           DECIMAL(10, 7) NULL,
    start_date          DATE         NOT NULL,
    end_date            DATE         NOT NULL,
    accommodation_name  VARCHAR(200) NULL,
    accommodation_place_id VARCHAR(255) NULL,
    check_in_at         DATETIME(6)  NULL,
    check_out_at        DATETIME(6)  NULL,
    transport_mode      VARCHAR(40)  NULL,
    departure_at        DATETIME(6)  NULL,
    arrival_at          DATETIME(6)  NULL,
    sort_order          INT          NOT NULL,
    created_by          VARCHAR(36)  NOT NULL,
    updated_by          VARCHAR(36)  NOT NULL,
    created_at          DATETIME(6)  NOT NULL,
    updated_at          DATETIME(6)  NOT NULL,
    version             BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_trip_segments PRIMARY KEY (id),
    CONSTRAINT fk_trip_segments_trip FOREIGN KEY (trip_id) REFERENCES trips (id),
    INDEX ix_trip_segments_trip_order (trip_id, sort_order)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE destination_candidates
(
    id            VARCHAR(36)  NOT NULL,
    trip_id       VARCHAR(36)  NOT NULL,
    name          VARCHAR(160) NOT NULL,
    country_code  CHAR(2)      NULL,
    place_id      VARCHAR(255) NULL,
    latitude      DECIMAL(10, 7) NULL,
    longitude     DECIMAL(10, 7) NULL,
    note          TEXT         NULL,
    status        VARCHAR(30)  NOT NULL,
    created_by    VARCHAR(36)  NOT NULL,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    version       BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_destination_candidates PRIMARY KEY (id),
    CONSTRAINT fk_destination_candidates_trip FOREIGN KEY (trip_id) REFERENCES trips (id),
    INDEX ix_destination_candidates_trip (trip_id, created_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE destination_candidate_preferences
(
    candidate_id VARCHAR(36) NOT NULL,
    user_id      VARCHAR(36) NOT NULL,
    preference   VARCHAR(20) NOT NULL,
    updated_at   DATETIME(6) NOT NULL,
    CONSTRAINT pk_destination_candidate_preferences PRIMARY KEY (candidate_id, user_id),
    CONSTRAINT fk_destination_preferences_candidate
        FOREIGN KEY (candidate_id) REFERENCES destination_candidates (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE date_candidates
(
    id          VARCHAR(36) NOT NULL,
    trip_id     VARCHAR(36) NOT NULL,
    start_date  DATE        NOT NULL,
    end_date    DATE        NOT NULL,
    note        TEXT        NULL,
    status      VARCHAR(30) NOT NULL,
    created_by  VARCHAR(36) NOT NULL,
    created_at  DATETIME(6) NOT NULL,
    updated_at  DATETIME(6) NOT NULL,
    version     BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT pk_date_candidates PRIMARY KEY (id),
    CONSTRAINT fk_date_candidates_trip FOREIGN KEY (trip_id) REFERENCES trips (id),
    INDEX ix_date_candidates_trip (trip_id, start_date)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE date_candidate_availability
(
    candidate_id VARCHAR(36) NOT NULL,
    user_id      VARCHAR(36) NOT NULL,
    availability VARCHAR(20) NOT NULL,
    note         VARCHAR(500) NULL,
    updated_at   DATETIME(6) NOT NULL,
    CONSTRAINT pk_date_candidate_availability PRIMARY KEY (candidate_id, user_id),
    CONSTRAINT fk_date_availability_candidate
        FOREIGN KEY (candidate_id) REFERENCES date_candidates (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
