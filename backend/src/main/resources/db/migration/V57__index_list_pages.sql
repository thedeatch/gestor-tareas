CREATE INDEX idx_tasks_user_page ON tasks (user_id, id DESC);
CREATE INDEX idx_community_posts_page ON community_posts (community_id, created_at DESC, id DESC);
