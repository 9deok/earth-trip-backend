ALTER TABLE trip_share_links
    ADD COLUMN public_content_json JSON NULL AFTER public_note;
