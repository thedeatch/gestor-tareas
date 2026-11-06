ALTER TABLE community_events ADD COLUMN created_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE community_events ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE community_albums ADD COLUMN created_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE community_albums ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP;

CREATE INDEX idx_community_events_activity ON community_events (community_id, created_at DESC, id DESC);
CREATE INDEX idx_community_albums_activity ON community_albums (community_id, created_at DESC, id DESC);