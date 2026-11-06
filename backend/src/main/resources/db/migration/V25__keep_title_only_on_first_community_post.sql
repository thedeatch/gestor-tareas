UPDATE community_posts
SET title = NULL
WHERE id NOT IN (
    SELECT MIN(id)
    FROM community_posts
    GROUP BY community_id
);
