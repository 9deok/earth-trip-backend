ALTER TABLE trips
    ADD COLUMN companion_count INT NOT NULL DEFAULT 1 AFTER pace,
    ADD COLUMN companion_names_json TEXT NULL AFTER companion_count,
    ADD COLUMN date_mode VARCHAR(30) NOT NULL DEFAULT 'EXACT' AFTER companion_names_json,
    ADD COLUMN travel_mode VARCHAR(30) NOT NULL DEFAULT 'ROUND_TRIP' AFTER date_mode,
    ADD COLUMN departure_point VARCHAR(200) NOT NULL DEFAULT '' AFTER travel_mode,
    ADD COLUMN return_point VARCHAR(200) NOT NULL DEFAULT '' AFTER departure_point,
    ADD COLUMN first_day_start_minutes INT NOT NULL DEFAULT 600 AFTER return_point,
    ADD COLUMN last_day_end_minutes INT NOT NULL DEFAULT 1080 AFTER first_day_start_minutes,
    ADD COLUMN overnight_travel_nights INT NOT NULL DEFAULT 0 AFTER last_day_end_minutes,
    ADD COLUMN reduce_stairs BOOLEAN NOT NULL DEFAULT FALSE AFTER overnight_travel_nights,
    ADD COLUMN frequent_breaks BOOLEAN NOT NULL DEFAULT TRUE AFTER reduce_stairs,
    ADD COLUMN walking_limit_minutes INT NOT NULL DEFAULT 90 AFTER frequent_breaks,
    ADD COLUMN dietary_notes VARCHAR(2000) NOT NULL DEFAULT '' AFTER walking_limit_minutes;

UPDATE trips
SET companion_names_json = '["나"]'
WHERE companion_names_json IS NULL OR companion_names_json = '';

ALTER TABLE trips
    MODIFY COLUMN companion_names_json TEXT NOT NULL;
