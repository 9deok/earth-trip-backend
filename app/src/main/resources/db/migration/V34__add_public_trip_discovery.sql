ALTER TABLE trip_share_links
    ADD COLUMN visibility VARCHAR(20) NOT NULL DEFAULT 'LINK_ONLY' AFTER projection_user_id,
    ADD COLUMN public_note VARCHAR(500) NULL AFTER visibility,
    ADD INDEX ix_trip_share_links_public (visibility, status, updated_at);
