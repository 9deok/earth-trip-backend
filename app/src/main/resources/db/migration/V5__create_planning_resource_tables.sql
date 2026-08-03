CREATE TABLE planning_resources
(
    id            VARCHAR(36) NOT NULL,
    trip_id       VARCHAR(36) NOT NULL,
    resource_type VARCHAR(50) NOT NULL,
    parent_id     VARCHAR(36) NULL,
    local_date    DATE        NULL,
    payload       JSON        NOT NULL,
    status        VARCHAR(40) NOT NULL,
    sort_order    INT         NOT NULL DEFAULT 0,
    created_by    VARCHAR(36) NOT NULL,
    updated_by    VARCHAR(36) NOT NULL,
    created_at    DATETIME(6) NOT NULL,
    updated_at    DATETIME(6) NOT NULL,
    deleted_at    DATETIME(6) NULL,
    version       BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT pk_planning_resources PRIMARY KEY (id),
    CONSTRAINT fk_planning_resources_trip FOREIGN KEY (trip_id) REFERENCES trips (id),
    INDEX ix_planning_resources_trip_type (trip_id, resource_type, deleted_at, sort_order),
    INDEX ix_planning_resources_parent (parent_id, resource_type, deleted_at),
    INDEX ix_planning_resources_local_date (trip_id, local_date, resource_type)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE planning_user_states
(
    resource_id VARCHAR(36) NOT NULL,
    user_id     VARCHAR(36) NOT NULL,
    state_type  VARCHAR(40) NOT NULL,
    value_json  JSON        NOT NULL,
    updated_at  DATETIME(6) NOT NULL,
    CONSTRAINT pk_planning_user_states PRIMARY KEY (resource_id, user_id, state_type),
    CONSTRAINT fk_planning_user_states_resource FOREIGN KEY (resource_id) REFERENCES planning_resources (id),
    INDEX ix_planning_user_states_user (user_id, state_type)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE planning_activity_events
(
    sequence_id BIGINT       NOT NULL AUTO_INCREMENT,
    event_id    VARCHAR(36)  NOT NULL,
    trip_id     VARCHAR(36)  NOT NULL,
    actor_id    VARCHAR(36)  NOT NULL,
    action      VARCHAR(80)  NOT NULL,
    resource_type VARCHAR(50) NOT NULL,
    resource_id VARCHAR(36)  NOT NULL,
    payload     JSON         NOT NULL,
    occurred_at DATETIME(6)  NOT NULL,
    CONSTRAINT pk_planning_activity_events PRIMARY KEY (sequence_id),
    CONSTRAINT uk_planning_activity_event_id UNIQUE (event_id),
    CONSTRAINT fk_planning_activity_trip FOREIGN KEY (trip_id) REFERENCES trips (id),
    INDEX ix_planning_activity_trip_sequence (trip_id, sequence_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE planning_operation_results
(
    operation_id VARCHAR(36) NOT NULL,
    trip_id      VARCHAR(36) NOT NULL,
    actor_id     VARCHAR(36) NOT NULL,
    status       VARCHAR(30) NOT NULL,
    resource_type VARCHAR(50) NULL,
    resource_id  VARCHAR(36) NULL,
    result_json  JSON        NOT NULL,
    created_at   DATETIME(6) NOT NULL,
    CONSTRAINT pk_planning_operation_results PRIMARY KEY (operation_id),
    CONSTRAINT fk_planning_operations_trip FOREIGN KEY (trip_id) REFERENCES trips (id),
    INDEX ix_planning_operations_trip_created (trip_id, created_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
