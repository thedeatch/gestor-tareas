ALTER TABLE communities ADD COLUMN image_file_id BIGINT;

ALTER TABLE communities ADD CONSTRAINT fk_community_image_file
    FOREIGN KEY (image_file_id) REFERENCES stored_files(id);

CREATE UNIQUE INDEX uk_community_image_file
    ON communities(image_file_id);
