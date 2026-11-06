ALTER TABLE community_posts ADD COLUMN title VARCHAR(150);

UPDATE community_posts SET title = 'Publicación' WHERE title IS NULL;

ALTER TABLE community_posts ALTER COLUMN title SET NOT NULL;
