CREATE TABLE community_image_likes (
    image_id BIGINT NOT NULL REFERENCES community_album_images(file_id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (image_id, user_id)
);
