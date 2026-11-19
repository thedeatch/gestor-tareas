package cl.gestortareas.api.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import cl.gestortareas.api.model.AppUser;
import cl.gestortareas.api.model.CommunityMembership;
import cl.gestortareas.api.model.CommunityRole;
import cl.gestortareas.api.repository.AppUserRepository;
import cl.gestortareas.api.repository.CommunityMembershipRepository;
import cl.gestortareas.api.repository.CommunityRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SavedItemsControllerIntegrationTests {
    private static final String OWNER = "saved-owner@community.cl";
    private static final String MEMBER = "saved-member@community.cl";
    private static final String OUTSIDER = "saved-outsider@community.cl";
    private static final String URL = "/api/saved-items";
    @Autowired MockMvc mvc;
    @Autowired JdbcClient jdbc;
    @Autowired AppUserRepository users;
    @Autowired CommunityRepository communities;
    @Autowired CommunityMembershipRepository memberships;
    private Long groupId;
    private Long postId;
    private Long imageId;
    private Long memberId;

    @BeforeEach
    void setup() throws Exception {
        users.save(new AppUser("Organizadora", OWNER, "hash"));
        var member = users.save(new AppUser("Miembro", MEMBER, "hash"));
        memberId = member.getId();
        users.save(new AppUser("Visitante", OUTSIDER, "hash"));
        mvc.perform(post("/api/communities").with(jwt().jwt(token -> token.subject(OWNER)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Grupo guardados\",\"description\":\"Pruebas\",\"visibility\":\"PRIVATE\"}"))
            .andExpect(status().isCreated());
        groupId = jdbc.sql("SELECT MAX(id) FROM communities").query(Long.class).single();
        memberships.saveAndFlush(new CommunityMembership(communities.findById(groupId).orElseThrow(),
            member, CommunityRole.MEMBER));
        mvc.perform(post("/api/communities/" + groupId + "/posts")
                .with(jwt().jwt(token -> token.subject(OWNER))).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Guía del grupo\",\"content\":\"Contenido para consultar después\"}"))
            .andExpect(status().isCreated());
        postId = jdbc.sql("SELECT id FROM community_posts WHERE community_id = :id")
            .param("id", groupId).query(Long.class).single();
        String albums = "/api/communities/" + groupId + "/albums";
        mvc.perform(post(albums).with(jwt().jwt(token -> token.subject(OWNER))).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Encuentro\",\"description\":\"Fotos\"}")).andExpect(status().isCreated());
        Long albumId = jdbc.sql("SELECT id FROM community_albums WHERE community_id = :id")
            .param("id", groupId).query(Long.class).single();
        byte[] png = java.util.Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC1lEQVR42mP8/x8AAwMCAO+aN1sAAAAASUVORK5CYII=");
        mvc.perform(multipart(albums + "/" + albumId + "/images")
                .file(new MockMultipartFile("file", "foto.png", "image/png", png))
                .with(jwt().jwt(token -> token.subject(OWNER)))).andExpect(status().isCreated());
        imageId = jdbc.sql("SELECT file_id FROM community_album_images WHERE album_id = :id")
            .param("id", albumId).query(Long.class).single();
    }

    @Test
    void savesPostsAndPhotosPrivatelyAndRepeatedRequestsKeepOneBookmark() throws Exception {
        save("POST", postId, MEMBER);
        save("POST", postId, MEMBER);
        save("IMAGE", imageId, MEMBER);
        mvc.perform(get(URL).with(jwt().jwt(token -> token.subject(MEMBER))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(2))
            .andExpect(jsonPath("$.size").value(20));
        mvc.perform(get(URL).param("kind", "POST").with(jwt().jwt(token -> token.subject(MEMBER))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.items[0].kind").value("POST"))
            .andExpect(jsonPath("$.items[0].title").value("Guía del grupo"))
            .andExpect(jsonPath("$.items[0].excerpt").value("Contenido para consultar después"))
            .andExpect(jsonPath("$.items[0].savedAt").isNotEmpty());
        mvc.perform(get(URL).param("kind", "IMAGE").with(jwt().jwt(token -> token.subject(MEMBER))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.items[0].kind").value("IMAGE"))
            .andExpect(jsonPath("$.items[0].title").value("foto.png"))
            .andExpect(jsonPath("$.items[0].albumId").isNumber());
        mvc.perform(get(URL).with(jwt().jwt(token -> token.subject(OWNER))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
        mvc.perform(get(URL + "/state").param("groupId", groupId.toString())
                .with(jwt().jwt(token -> token.subject(MEMBER))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.postIds[0]").value(postId))
            .andExpect(jsonPath("$.imageIds[0]").value(imageId));
        save("POST", postId, OWNER);
        mvc.perform(delete(URL + "/POST/" + postId).with(jwt().jwt(token -> token.subject(MEMBER))))
            .andExpect(status().isNoContent());
        mvc.perform(delete(URL + "/POST/" + postId).with(jwt().jwt(token -> token.subject(MEMBER))))
            .andExpect(status().isNoContent());
        mvc.perform(get(URL).param("kind", "POST").with(jwt().jwt(token -> token.subject(OWNER))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void protectsGroupContentAndHidesItemsAfterMembershipEnds() throws Exception {
        mvc.perform(put(URL + "/POST/" + postId).with(jwt().jwt(token -> token.subject(OUTSIDER)))
                .contentType(MediaType.APPLICATION_JSON).content("{\"groupId\":" + groupId + "}"))
            .andExpect(status().isNotFound());
        mvc.perform(get(URL + "/state").param("groupId", groupId.toString())
                .with(jwt().jwt(token -> token.subject(OUTSIDER)))).andExpect(status().isNotFound());
        mvc.perform(put(URL + "/POST/0").with(jwt().jwt(token -> token.subject(MEMBER)))
                .contentType(MediaType.APPLICATION_JSON).content("{\"groupId\":" + groupId + "}"))
            .andExpect(status().isNotFound());
        save("POST", postId, MEMBER);
        save("IMAGE", imageId, MEMBER);
        jdbc.sql("DELETE FROM community_memberships WHERE community_id = :groupId AND user_id = :userId")
            .param("groupId", groupId).param("userId", memberId).update();
        mvc.perform(get(URL).with(jwt().jwt(token -> token.subject(MEMBER))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
        mvc.perform(delete(URL + "/POST/" + postId).with(jwt().jwt(token -> token.subject(MEMBER))))
            .andExpect(status().isNoContent());
        jdbc.sql("DELETE FROM community_album_images WHERE file_id = :id").param("id", imageId).update();
        assertEquals(0L, jdbc.sql("SELECT COUNT(*) FROM saved_items WHERE user_id = :id")
            .param("id", memberId).query(Long.class).single());
    }

    @Test
    void paginatesWithoutReturningAnotherPersonsBookmarks() throws Exception {
        for (int index = 0; index < 21; index++) {
            var key = new GeneratedKeyHolder();
            jdbc.sql("""
                INSERT INTO community_posts (community_id, author_user_id, content, created_at, updated_at)
                VALUES (:groupId, :author, :content, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """).param("groupId", groupId).param("author", memberId).param("content", "Nota " + index)
                .update(key, "id");
            jdbc.sql("INSERT INTO saved_items (user_id, post_id) VALUES (:userId, :postId)")
                .param("userId", memberId).param("postId", key.getKeyAs(Long.class)).update();
        }
        mvc.perform(get(URL).with(jwt().jwt(token -> token.subject(MEMBER))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(21))
            .andExpect(jsonPath("$.items.length()").value(20));
        mvc.perform(get(URL).param("page", "1").with(jwt().jwt(token -> token.subject(MEMBER))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1));
        mvc.perform(get(URL).param("page", "-1").with(jwt().jwt(token -> token.subject(MEMBER))))
            .andExpect(status().isBadRequest());
    }

    private void save(String kind, Long targetId, String email) throws Exception {
        mvc.perform(put(URL + "/" + kind + "/" + targetId).with(jwt().jwt(token -> token.subject(email)))
                .contentType(MediaType.APPLICATION_JSON).content("{\"groupId\":" + groupId + "}"))
            .andExpect(status().isNoContent());
    }

    @Test
    void savesEventsPrivatelyWithoutRegisteringAndCascadesDeletedEvents() throws Exception {
        Long event = addEvent(groupId);
        save("EVENT", event, MEMBER);
        save("EVENT", event, MEMBER);
        mvc.perform(get(URL).param("kind", "EVENT").with(jwt().jwt(t -> t.subject(MEMBER))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.items[0].kind").value("EVENT"))
            .andExpect(jsonPath("$.items[0].targetId").value(event))
            .andExpect(jsonPath("$.items[0].title").value("Encuentro anual"));
        mvc.perform(get(URL + "/state").param("groupId", groupId.toString()).with(jwt().jwt(t -> t.subject(MEMBER))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.eventIds[0]").value(event));
        mvc.perform(get(URL).with(jwt().jwt(t -> t.subject(OWNER))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
        assertEquals(0L, jdbc.sql("SELECT COUNT(*) FROM community_event_registrations WHERE event_id = :id")
            .param("id", event).query(Long.class).single());
        mvc.perform(delete(URL + "/EVENT/" + event).with(jwt().jwt(t -> t.subject(MEMBER))))
            .andExpect(status().isNoContent());
        mvc.perform(delete(URL + "/EVENT/" + event).with(jwt().jwt(t -> t.subject(MEMBER))))
            .andExpect(status().isNoContent());
        save("EVENT", event, MEMBER);
        jdbc.sql("DELETE FROM community_events WHERE id = :id").param("id", event).update();
        mvc.perform(get(URL).with(jwt().jwt(t -> t.subject(MEMBER))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
        assertEquals(0L, jdbc.sql("SELECT COUNT(*) FROM saved_event_items WHERE user_id = :id")
            .param("id", memberId).query(Long.class).single());
    }

    @Test
    void protectsEventMembershipAndRejectsEventsFromAnotherGroup() throws Exception {
        Long event = addEvent(groupId);
        mvc.perform(put(URL + "/EVENT/" + event).with(jwt().jwt(t -> t.subject(OUTSIDER)))
            .contentType(MediaType.APPLICATION_JSON).content("{\"groupId\":" + groupId + "}"))
            .andExpect(status().isNotFound());
        mvc.perform(post("/api/communities").with(jwt().jwt(t -> t.subject(OWNER)))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Otro grupo\",\"description\":\"Pruebas\",\"visibility\":\"PRIVATE\"}"))
            .andExpect(status().isCreated());
        Long other = jdbc.sql("SELECT MAX(id) FROM communities").query(Long.class).single();
        mvc.perform(put(URL + "/EVENT/" + event).with(jwt().jwt(t -> t.subject(OWNER)))
            .contentType(MediaType.APPLICATION_JSON).content("{\"groupId\":" + other + "}"))
            .andExpect(status().isNotFound());
        save("EVENT", event, MEMBER);
        jdbc.sql("DELETE FROM community_memberships WHERE community_id = :groupId AND user_id = :userId")
            .param("groupId", groupId).param("userId", memberId).update();
        mvc.perform(get(URL).with(jwt().jwt(t -> t.subject(MEMBER))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
        mvc.perform(delete(URL + "/EVENT/" + event).with(jwt().jwt(t -> t.subject(MEMBER))))
            .andExpect(status().isNoContent());
    }

    private Long addEvent(Long group) {
        var key = new GeneratedKeyHolder();
        jdbc.sql("INSERT INTO community_events (community_id, title, description, starts_at) VALUES (:groupId, 'Encuentro anual', 'Actividad del grupo', CURRENT_TIMESTAMP)")
            .param("groupId", group).update(key, "id");
        return key.getKeyAs(Long.class);
    }
}
