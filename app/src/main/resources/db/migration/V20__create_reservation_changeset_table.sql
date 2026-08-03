CREATE TABLE reservation_changesets
(
    id               VARCHAR(36) NOT NULL,
    trip_id          VARCHAR(36) NOT NULL,
    reservation_id   VARCHAR(36) NOT NULL,
    requested_by     VARCHAR(36) NOT NULL,
    proposal_hash    VARCHAR(64) NOT NULL,
    before_snapshot  JSON        NOT NULL,
    after_snapshot   JSON        NOT NULL,
    applied_at       DATETIME(6) NOT NULL,
    CONSTRAINT pk_reservation_changesets PRIMARY KEY (id),
    CONSTRAINT fk_reservation_changesets_trip FOREIGN KEY (trip_id) REFERENCES trips (id),
    INDEX ix_reservation_changesets_reservation (reservation_id, applied_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
