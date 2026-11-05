UPDATE users
SET theme_preference = 'LIGHT'
WHERE theme_preference = 'SYSTEM';

ALTER TABLE users
    ALTER COLUMN theme_preference SET DEFAULT 'LIGHT';
