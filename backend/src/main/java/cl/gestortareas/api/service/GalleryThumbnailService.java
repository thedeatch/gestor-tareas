package cl.gestortareas.api.service;

import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.function.Supplier;
import javax.imageio.ImageIO;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GalleryThumbnailService {
    private static final int EDGE = 480;
    private final JdbcClient jdbc;

    public GalleryThumbnailService(JdbcClient jdbc) { this.jdbc = jdbc; }

    // Called within CommunityAlbumService's transaction, after membership and album checks.
    byte[] readOrCreate(Long imageId, Supplier<byte[]> source) {
        var cached = cached(imageId);
        if (cached != null) return cached;
        jdbc.sql("SELECT file_id FROM community_album_images WHERE file_id = :id FOR UPDATE")
            .param("id", imageId).query(Long.class).optional().orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Imagen no encontrada"));
        cached = cached(imageId);
        if (cached != null) return cached;
        byte[] thumbnail = generate(source.get());
        jdbc.sql("INSERT INTO community_image_thumbnails (image_id, data) VALUES (:id, :data)")
            .param("id", imageId).param("data", thumbnail).update();
        return thumbnail;
    }

    private byte[] cached(Long imageId) {
        return jdbc.sql("SELECT data FROM community_image_thumbnails WHERE image_id = :id")
            .param("id", imageId).query((rs, row) -> rs.getBytes("data")).optional().orElse(null);
    }

    static byte[] generate(byte[] original) {
        try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(original))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IOException("Unsupported image");
            var reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long) width * height > 80_000_000L) {
                    throw new IOException("Unsupported image dimensions");
                }
                var params = reader.getDefaultReadParam();
                // Subsample before decoding to avoid allocating a full-resolution raster.
                int sample = Math.max(1, (int) Math.ceil(Math.max(width, height) / (double) EDGE));
                params.setSourceSubsampling(sample, sample, 0, 0);
                BufferedImage decoded = reader.read(0, params);
                double scale = Math.min(1, EDGE / (double) Math.max(decoded.getWidth(), decoded.getHeight()));
                var thumbnail = new BufferedImage(Math.max(1, (int) Math.round(decoded.getWidth() * scale)),
                    Math.max(1, (int) Math.round(decoded.getHeight() * scale)), BufferedImage.TYPE_INT_RGB);
                var graphics = thumbnail.createGraphics();
                try {
                    graphics.setColor(Color.WHITE);
                    graphics.fillRect(0, 0, thumbnail.getWidth(), thumbnail.getHeight());
                    graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    graphics.drawImage(decoded, 0, 0, thumbnail.getWidth(), thumbnail.getHeight(), null);
                } finally { graphics.dispose(); decoded.flush(); }
                try (var output = new ByteArrayOutputStream()) {
                    if (!ImageIO.write(thumbnail, "jpeg", output)) throw new IOException("JPEG encoder unavailable");
                    return output.toByteArray();
                } finally { thumbnail.flush(); }
            } finally { reader.dispose(); }
        } catch (IOException | IllegalArgumentException error) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT, "No se pudo generar la miniatura", error);
        }
    }
}
