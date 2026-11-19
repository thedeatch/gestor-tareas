package cl.gestortareas.api.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Random;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import cl.gestortareas.api.model.AppUser;
import cl.gestortareas.api.repository.AppUserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class GalleryThumbnailControllerIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired JdbcClient jdbc;
    @Autowired AppUserRepository users;
    private static final String OWNER = "thumb-owner@test.cl";
    private static final String OUTSIDER = "thumb-outsider@test.cl";
    private Long groupId;
    private Long albumId;
    private String images;

    @BeforeEach
    void setup() throws Exception {
        users.saveAndFlush(new AppUser("Propietaria", OWNER, "hash"));
        users.saveAndFlush(new AppUser("Visitante", OUTSIDER, "hash"));
        mvc.perform(post("/api/communities").with(jwt().jwt(t -> t.subject(OWNER)))
            .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Miniaturas\",\"description\":\"Prueba\",\"visibility\":\"PRIVATE\"}"))
            .andExpect(status().isCreated());
        groupId = jdbc.sql("SELECT MAX(id) FROM communities").query(Long.class).single();
        String albums = "/api/communities/" + groupId + "/albums";
        mvc.perform(post(albums).with(jwt().jwt(t -> t.subject(OWNER))).contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\":\"Recuerdos\",\"description\":\"\"}")).andExpect(status().isCreated());
        albumId = jdbc.sql("SELECT MAX(id) FROM community_albums").query(Long.class).single();
        images = albums + "/" + albumId + "/images";
    }

    @Test
    void generatesSmallCachedThumbnailsWhilePreservingTheOriginalAndAuthorization() throws Exception {
        var original = new BufferedImage(900, 600, BufferedImage.TYPE_INT_RGB);
        var random = new Random(42);
        for (int y = 0; y < 600; y++) for (int x = 0; x < 900; x++) original.setRGB(x, y, random.nextInt());
        var output = new ByteArrayOutputStream();
        ImageIO.write(original, "png", output);
        byte[] png = output.toByteArray();
        Long id = upload(png);
        String url = images + "/" + id;
        mvc.perform(get(url + "/thumbnail")).andExpect(status().isUnauthorized());
        byte[] thumbnail = mvc.perform(get(url + "/thumbnail").with(jwt().jwt(t -> t.subject(OWNER))))
            .andExpect(status().isOk()).andExpect(content().contentType(MediaType.IMAGE_JPEG))
            .andReturn().getResponse().getContentAsByteArray();
        var decoded = ImageIO.read(new ByteArrayInputStream(thumbnail));
        assertTrue(decoded.getWidth() <= 480 && decoded.getHeight() <= 480);
        assertEquals(1.5, decoded.getWidth() / (double) decoded.getHeight(), 0.01);
        assertTrue(thumbnail.length < png.length / 4);
        byte[] cached = mvc.perform(get(url + "/thumbnail").with(jwt().jwt(t -> t.subject(OWNER))))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        assertArrayEquals(thumbnail, cached);
        assertEquals(1L, jdbc.sql("SELECT COUNT(*) FROM community_image_thumbnails WHERE image_id = :id").param("id", id).query(Long.class).single());
        mvc.perform(get(url).with(jwt().jwt(t -> t.subject(OWNER))))
            .andExpect(status().isOk()).andExpect(content().bytes(png));
        mvc.perform(get(url + "/thumbnail").with(jwt().jwt(t -> t.subject(OUTSIDER))))
            .andExpect(status().isNotFound());
        mvc.perform(get("/api/communities/" + groupId + "/albums/0/images/" + id + "/thumbnail")
            .with(jwt().jwt(t -> t.subject(OWNER)))).andExpect(status().isNotFound());
        jdbc.sql("DELETE FROM community_album_images WHERE file_id = :id").param("id", id).update();
        assertEquals(0L, jdbc.sql("SELECT COUNT(*) FROM community_image_thumbnails WHERE image_id = :id").param("id", id).query(Long.class).single());
    }

    @Test
    void returnsAnExplicitErrorForUndecodableImagesWithoutCachingABrokenPreview() throws Exception {
        Long id = upload(new byte[] { (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a });
        mvc.perform(get(images + "/" + id + "/thumbnail").with(jwt().jwt(t -> t.subject(OWNER))))
            .andExpect(status().isUnprocessableEntity());
        assertEquals(0L, jdbc.sql("SELECT COUNT(*) FROM community_image_thumbnails WHERE image_id = :id").param("id", id).query(Long.class).single());
    }

    private Long upload(byte[] bytes) throws Exception {
        mvc.perform(multipart(images).file(new MockMultipartFile("file", "photo.png", "image/png", bytes))
            .with(jwt().jwt(t -> t.subject(OWNER)))).andExpect(status().isCreated());
        return jdbc.sql("SELECT MAX(file_id) FROM community_album_images").query(Long.class).single();
    }
}
