ALTER TABLE users ADD COLUMN name VARCHAR(100);

UPDATE users
SET name = CASE
    WHEN POSITION('@' IN email) > 1
        THEN SUBSTRING(email, 1, POSITION('@' IN email) - 1)
    ELSE SUBSTRING(email, 1, 100)
END;

ALTER TABLE users ALTER COLUMN name SET NOT NULL;

