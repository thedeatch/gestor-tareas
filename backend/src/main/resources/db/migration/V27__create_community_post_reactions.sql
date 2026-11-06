CREATE TABLE community_post_reactions (
    post_id BIGINT NOT NULL REFERENCES community_posts(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(20) NOT NULL CHECK (type IN ('LIKE', 'LOVE', 'CELEBRATE')),
    PRIMARY KEY (post_id, user_id, type)
);