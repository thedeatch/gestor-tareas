CREATE TABLE community_access_requests (
    id BIGSERIAL PRIMARY KEY,
    community_id BIGINT NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_by_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP WITH TIME ZONE,
    resolved_by_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT uk_community_access_request UNIQUE (community_id, user_id, type)
);

CREATE INDEX idx_community_access_group_status
    ON community_access_requests(community_id, type, status, created_at DESC);
CREATE INDEX idx_community_access_user_status
    ON community_access_requests(user_id, type, status, created_at DESC);
