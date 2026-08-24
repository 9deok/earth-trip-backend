ALTER TABLE trip_segments
    ADD COLUMN time_zone VARCHAR(80) NULL AFTER longitude,
    ADD COLUMN anchor_at DATETIME(6) NULL AFTER arrival_at;
