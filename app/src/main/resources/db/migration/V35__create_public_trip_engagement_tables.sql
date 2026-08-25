CREATE TABLE public_trip_reactions
(
    publication_id VARCHAR(36) NOT NULL,
    actor_user_id   VARCHAR(36) NOT NULL,
    reaction_type  VARCHAR(20) NOT NULL,
    created_at     DATETIME(6) NOT NULL,
    updated_at     DATETIME(6) NOT NULL,
    CONSTRAINT pk_public_trip_reactions PRIMARY KEY (publication_id, actor_user_id, reaction_type),
    CONSTRAINT fk_public_trip_reactions_publication FOREIGN KEY (publication_id) REFERENCES trip_share_links (id),
    INDEX ix_public_trip_reactions_count (publication_id, reaction_type)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE public_trip_comments
(
    id             VARCHAR(36)  NOT NULL,
    publication_id VARCHAR(36)  NOT NULL,
    actor_user_id  VARCHAR(36)  NOT NULL,
    body           VARCHAR(800) NOT NULL,
    status         VARCHAR(20)  NOT NULL,
    created_at     DATETIME(6)  NOT NULL,
    updated_at     DATETIME(6)  NOT NULL,
    CONSTRAINT pk_public_trip_comments PRIMARY KEY (id),
    CONSTRAINT fk_public_trip_comments_publication FOREIGN KEY (publication_id) REFERENCES trip_share_links (id),
    INDEX ix_public_trip_comments_publication (publication_id, status, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
