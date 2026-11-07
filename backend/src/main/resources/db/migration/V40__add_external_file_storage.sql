ALTER TABLE stored_files ADD COLUMN object_key VARCHAR(255);
ALTER TABLE stored_files ALTER COLUMN data DROP NOT NULL;

ALTER TABLE stored_files ADD CONSTRAINT ck_stored_file_location CHECK (
    (object_key IS NULL AND data IS NOT NULL) OR (object_key IS NOT NULL AND data IS NULL)
);