CREATE INDEX idx_community_album_images_album_file
    ON community_album_images (album_id, file_id DESC);
