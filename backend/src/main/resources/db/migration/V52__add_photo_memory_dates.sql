ALTER TABLE community_album_images ADD COLUMN taken_on DATE;
ALTER TABLE community_album_images ADD COLUMN taken_year INTEGER;
ALTER TABLE community_album_images ADD CONSTRAINT ck_photo_memory_date
    CHECK (taken_on IS NULL OR taken_year IS NULL);
ALTER TABLE community_album_images ADD CONSTRAINT ck_photo_memory_year
    CHECK (taken_year IS NULL OR taken_year BETWEEN 1 AND 9999);
