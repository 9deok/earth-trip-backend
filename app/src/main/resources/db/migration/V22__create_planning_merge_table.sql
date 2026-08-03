CREATE TABLE planning_resource_merges
(
    id              VARCHAR(36) NOT NULL,
    trip_id         VARCHAR(36) NOT NULL,
    resource_type   VARCHAR(50) NOT NULL,
    primary_id      VARCHAR(36) NOT NULL,
    duplicate_ids   JSON        NOT NULL,
    before_snapshot JSON        NOT NULL,
    after_snapshot  JSON        NOT NULL,
    added_links     JSON        NOT NULL,
    status          VARCHAR(20) NOT NULL,
    merged_by       VARCHAR(36) NOT NULL,
    merged_at       DATETIME(6) NOT NULL,
    reverted_by     VARCHAR(36) NULL,
    reverted_at     DATETIME(6) NULL,
    version         BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT pk_planning_resource_merges PRIMARY KEY (id),
    CONSTRAINT fk_planning_resource_merges_trip FOREIGN KEY (trip_id) REFERENCES trips (id),
    INDEX ix_planning_resource_merges_trip_type (trip_id, resource_type, merged_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
