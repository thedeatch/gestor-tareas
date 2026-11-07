CREATE TABLE community_event_reminders (
    event_id BIGINT NOT NULL REFERENCES community_events(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (event_id, user_id)
);

CREATE INDEX idx_community_events_reminder_window ON community_events (starts_at, id);