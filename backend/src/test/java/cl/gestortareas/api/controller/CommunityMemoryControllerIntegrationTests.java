package cl.gestortareas.api.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
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
import cl.gestortareas.api.model.CommunityMembership;
import cl.gestortareas.api.model.CommunityRole;
import cl.gestortareas.api.repository.AppUserRepository;
import cl.gestortareas.api.repository.CommunityRepository;
import cl.gestortareas.api.repository.CommunityMembershipRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CommunityMemoryControllerIntegrationTests {
    private static final String OWNER = "memory-owner@community.cl";
    private static final String MEMBER = "memory-member@community.cl";
    private static final String OUTSIDER = "memory-outsider@community.cl";
    @Autowired MockMvc mvc;
    @Autowired JdbcClient jdbc;
    @Autowired AppUserRepository users;
    @Autowired CommunityRepository communities;
    @Autowired CommunityMembershipRepository memberships;
    private Long groupId;
    private Long albumId;
    private Long imageId;
    private Long ownerId;
    private String images;
    private String date;
    private String memories;
    private final LocalDate today = LocalDate.now(ZoneId.of("America/Santiago"));

    @BeforeEach
    void setup() throws Exception {
        ownerId = users.save(new AppUser("Organizadora", OWNER, "hash")).getId();
        var member = users.save(new AppUser("Miembro", MEMBER, "hash"));
        users.save(new AppUser("Visitante", OUTSIDER, "hash"));
        mvc.perform(post("/api/communities").with(jwt().jwt(token -> token.subject(OWNER)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Recuerdos\",\"description\":\"Fotos antiguas\",\"visibility\":\"PRIVATE\"}"))
            .andExpect(status().isCreated());
        groupId = jdbc.sql("SELECT MAX(id) FROM communities").query(Long.class).single();
        memberships.saveAndFlush(new CommunityMembership(communities.findById(groupId).orElseThrow(),
            member, CommunityRole.MEMBER));
        String albums = "/api/communities/" + groupId + "/albums";
        mvc.perform(post(albums).with(jwt().jwt(token -> token.subject(OWNER))).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Reencuentro\",\"description\":\"Fotos\"}")).andExpect(status().isCreated());
        albumId = jdbc.sql("SELECT id FROM community_albums WHERE community_id = :id")
            .param("id", groupId).query(Long.class).single();
        images = albums + "/" + albumId + "/images";
        memories = "/api/communities/" + groupId + "/memories";
        mvc.perform(multipart(images).file(photo()).with(jwt().jwt(token -> token.subject(OWNER))))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.date.uploadedOn").value(today.toString()))
            .andExpect(jsonPath("$.date.takenOn").isEmpty()).andExpect(jsonPath("$.date.takenYear").isEmpty());
        imageId = jdbc.sql("SELECT file_id FROM community_album_images WHERE album_id = :id")
            .param("id", albumId).query(Long.class).single();
        date = images + "/" + imageId + "/date";
    }

    @Test
    void realDatesAndYearOnlyControlMemoriesAndCanBeResetToTheOriginalUploadDate() throws Exception {
        mvc.perform(get(memories).with(jwt().jwt(token -> token.subject(MEMBER))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
        mvc.perform(patch(date).with(jwt().jwt(token -> token.subject(OWNER))).contentType(MediaType.APPLICATION_JSON)
                .content("{\"takenOn\":null,\"takenYear\":2016}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.date.years").value(today.getYear() - 2016));
        mvc.perform(get(memories).with(jwt().jwt(token -> token.subject(MEMBER))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].date.takenYear").value(2016))
            .andExpect(jsonPath("$.items[0].date.canEdit").value(false));
        mvc.perform(get(memories).param("anniversariesOnly", "true").with(jwt().jwt(token -> token.subject(OWNER))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
        String exact = LocalDate.of(2016, today.getMonthValue(), today.getDayOfMonth()).toString();
        mvc.perform(patch(date).with(jwt().jwt(token -> token.subject(OWNER))).contentType(MediaType.APPLICATION_JSON)
                .content("{\"takenOn\":\"" + exact + "\",\"takenYear\":null}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.date.anniversary").value(true));
        mvc.perform(get(memories).param("anniversariesOnly", "true").with(jwt().jwt(token -> token.subject(OWNER))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1));
        mvc.perform(patch(date).with(jwt().jwt(token -> token.subject(OWNER))).contentType(MediaType.APPLICATION_JSON)
                .content("{\"takenOn\":null,\"takenYear\":null}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.date.uploadedOn").value(today.toString()));
        mvc.perform(get(memories).with(jwt().jwt(token -> token.subject(OWNER))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void protectsMetadataAndSupportsOldUploadsWithoutAManualDate() throws Exception {
        mvc.perform(get(memories).with(jwt().jwt(token -> token.subject(OUTSIDER)))).andExpect(status().isNotFound());
        mvc.perform(patch(date).with(jwt().jwt(token -> token.subject(MEMBER))).contentType(MediaType.APPLICATION_JSON)
                .content("{\"takenYear\":2010}")).andExpect(status().isForbidden());
        for (String invalid : new String[] {
            "{\"takenOn\":\"2010-01-01\",\"takenYear\":2010}",
            "{\"takenOn\":\"" + today.plusDays(1) + "\"}",
            "{\"takenYear\":0}", "{\"takenYear\":9999}"
        }) {
            mvc.perform(patch(date).with(jwt().jwt(token -> token.subject(OWNER))).contentType(MediaType.APPLICATION_JSON)
                    .content(invalid)).andExpect(status().isBadRequest());
        }
        jdbc.sql("UPDATE stored_files SET created_at = :old WHERE id = :id")
            .param("old", OffsetDateTime.now().minusYears(5)).param("id", imageId).update();
        mvc.perform(get(memories).with(jwt().jwt(token -> token.subject(MEMBER))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.items[0].date.years").value(5));
        jdbc.sql("UPDATE community_memberships SET role = 'MEMBER' WHERE community_id = :groupId AND user_id = :userId")
            .param("groupId", groupId).param("userId", ownerId).update();
        mvc.perform(patch(date).with(jwt().jwt(token -> token.subject(OWNER))).contentType(MediaType.APPLICATION_JSON)
                .content("{\"takenYear\":2010}")).andExpect(status().isOk());
    }

    @Test
    void uploadsAcceptOptionalDatesAndRejectInvalidMetadataBeforeStoringFiles() throws Exception {
        mvc.perform(multipart(images).file(photo()).param("takenYear", "2010")
                .with(jwt().jwt(token -> token.subject(OWNER))))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.date.takenYear").value(2010));
        mvc.perform(multipart(images).file(photo()).param("takenYear", "9999")
                .with(jwt().jwt(token -> token.subject(OWNER)))).andExpect(status().isBadRequest());
        assertEquals(2L, jdbc.sql("SELECT COUNT(*) FROM community_album_images WHERE album_id = :id")
            .param("id", albumId).query(Long.class).single());
    }

    private MockMultipartFile photo() {
        return new MockMultipartFile("file", "recuerdo.png", "image/png", java.util.Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC1lEQVR42mP8/x8AAwMCAO+aN1sAAAAASUVORK5CYII="));
    }
}
