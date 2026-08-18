ALTER TABLE trip_segments
    MODIFY start_date DATE NULL,
    MODIFY end_date DATE NULL;

UPDATE trips
SET date_mode = 'UNDECIDED'
WHERE start_date IS NULL
  AND end_date IS NULL
  AND date_mode = 'EXACT';

ALTER TABLE trips
    MODIFY date_mode VARCHAR(30) NOT NULL DEFAULT 'UNDECIDED';
