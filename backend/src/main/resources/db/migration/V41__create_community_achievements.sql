CREATE TABLE community_achievement_progress (
    community_id BIGINT NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    posts BIGINT NOT NULL DEFAULT 0,
    comments BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (community_id, user_id)
);

CREATE TABLE community_achievements (
    community_id BIGINT NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    code VARCHAR(30) NOT NULL,
    earned_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (community_id, user_id, code)
);

INSERT INTO community_achievement_progress (community_id, user_id, posts, comments)
SELECT activity.community_id, activity.user_id, SUM(activity.posts), SUM(activity.comments)
FROM (
    SELECT community_id, author_user_id AS user_id, COUNT(*) AS posts, 0 AS comments
    FROM community_posts GROUP BY community_id, author_user_id
    UNION ALL
    SELECT p.community_id, c.author_user_id, 0, COUNT(*)
    FROM community_post_comments c JOIN community_posts p ON p.id = c.post_id
    GROUP BY p.community_id, c.author_user_id
) activity
GROUP BY activity.community_id, activity.user_id;

INSERT INTO community_achievements (community_id, user_id, code)
SELECT community_id, user_id, 'FIRST_POST' FROM community_achievement_progress WHERE posts >= 1
UNION ALL
SELECT community_id, user_id, 'TEN_COMMENTS' FROM community_achievement_progress WHERE comments >= 10
UNION ALL
SELECT community_id, user_id, 'FIFTY_POINTS' FROM community_achievement_progress WHERE posts * 5 + comments * 2 >= 50;