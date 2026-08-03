CREATE TABLE candidate_source_links
(
    candidate_id VARCHAR(36) NOT NULL,
    source_id    VARCHAR(36) NOT NULL,
    trip_id      VARCHAR(36) NOT NULL,
    linked_by    VARCHAR(36) NOT NULL,
    linked_at    DATETIME(6) NOT NULL,
    CONSTRAINT pk_candidate_source_links PRIMARY KEY (candidate_id, source_id),
    CONSTRAINT fk_candidate_source_link_candidate FOREIGN KEY (candidate_id)
        REFERENCES planning_resources (id),
    CONSTRAINT fk_candidate_source_link_source FOREIGN KEY (source_id)
        REFERENCES planning_resources (id),
    CONSTRAINT fk_candidate_source_link_trip FOREIGN KEY (trip_id) REFERENCES trips (id),
    INDEX ix_candidate_source_links_source (source_id, candidate_id),
    INDEX ix_candidate_source_links_trip (trip_id, linked_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
