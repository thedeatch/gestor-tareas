CREATE TABLE community_image_thumbnails (
    image_id BIGINT PRIMARY KEY REFERENCES community_album_images(file_id) ON DELETE CASCADE,
    data BYTEA NOT NULL
);
