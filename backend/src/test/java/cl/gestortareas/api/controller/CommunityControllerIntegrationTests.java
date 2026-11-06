package cl.gestortareas.api.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import org.springframework.test.web.servlet.MockMvc;

import cl.gestortareas.api.model.AppUser;
import cl.gestortareas.api.model.CommunityMembership;
import cl.gestortareas.api.model.CommunityRole;
import cl.gestortareas.api.repository.AppUserRepository;
import cl.gestortareas.api.repository.CommunityAccessRequestRepository;
import cl.gestortareas.api.repository.CommunityMembershipRepository;
import cl.gestortareas.api.repository.CommunityRepository;
import cl.gestortareas.api.repository.CommunityPostRepository;
import cl.gestortareas.api.repository.CommunityPostCommentRepository;
import cl.gestortareas.api.repository.NotificationRepository;

@SpringBootTest
@AutoConfigureMockMvc
class CommunityControllerIntegrationTests {
    private static final String OWNER_EMAIL = "owner@community.cl";
    private static final String OTHER_EMAIL = "other@community.cl";

    @Autowired MockMvc mockMvc;
    @Autowired org.springframework.jdbc.core.simple.JdbcClient jdbc;
    @Autowired CommunityRepository communityRepository;
    @Autowired CommunityPostRepository postRepository;
    @Autowired CommunityPostCommentRepository commentRepository;
    @Autowired CommunityMembershipRepository membershipRepository;
    @Autowired CommunityAccessRequestRepository accessRepository;
    @Autowired NotificationRepository notificationRepository;
    @Autowired AppUserRepository userRepository;

    @BeforeEach
    void cleanDatabase() {
        jdbc.sql("DELETE FROM community_moderation_log").update();
        notificationRepository.deleteAll();
        accessRepository.deleteAll();
        postRepository.deleteAll();
        membershipRepository.deleteAll();
        communityRepository.deleteAll();
        userRepository.deleteAll();
        userRepository.save(new AppUser("Propietaria", OWNER_EMAIL, "hash"));
        userRepository.save(new AppUser("Otra persona", OTHER_EMAIL, "hash"));
    }

    @Test
    void shouldCreateCommunityAndOwnerMembership() throws Exception {
        mockMvc.perform(post("/api/communities")
                .with(jwt().jwt(token -> token.subject(OWNER_EMAIL)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"Comunidad Angular","description":"Aprendizaje colaborativo","visibility":"PUBLIC"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Comunidad Angular"))
            .andExpect(jsonPath("$.slug").value("comunidad-angular"))
            .andExpect(jsonPath("$.currentUserRole").value("OWNER"))
            .andExpect(jsonPath("$.memberCount").value(1));

        assertEquals(1, communityRepository.count());
        assertEquals(1, membershipRepository.count());
    }

    @Test
    void shouldListOnlyCurrentUserCommunities() throws Exception {
        createCommunity();
        mockMvc.perform(get("/api/communities")
                .with(jwt().jwt(token -> token.subject(OTHER_EMAIL))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void shouldReturnCommunityDetailToMember() throws Exception {
        createCommunity();
        Long id = communityRepository.findAll().get(0).getId();
        mockMvc.perform(get("/api/communities/{id}", id)
                .with(jwt().jwt(token -> token.subject(OWNER_EMAIL))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.members.length()").value(1))
            .andExpect(jsonPath("$.members[0].role").value("OWNER"));
    }

    @Test
    void shouldHideCommunityFromNonMember() throws Exception {
        createCommunity();
        Long id = communityRepository.findAll().get(0).getId();
        mockMvc.perform(get("/api/communities/{id}", id)
                .with(jwt().jwt(token -> token.subject(OTHER_EMAIL))))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldApproveJoinRequestAndCreateMembership() throws Exception {
        createPublicCommunity();
        Long groupId = communityRepository.findAll().get(0).getId();

        mockMvc.perform(post("/api/communities/{id}/join-requests", groupId)
                .with(jwt().jwt(token -> token.subject(OTHER_EMAIL))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("PENDING"));

        Long requestId = accessRepository.findAll().get(0).getId();
        mockMvc.perform(patch(
                "/api/communities/{id}/join-requests/{requestId}/approve",
                groupId, requestId)
                .with(jwt().jwt(token -> token.subject(OWNER_EMAIL))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("APPROVED"));

        assertEquals(2, membershipRepository.countByCommunityId(groupId));
    }

    @Test
    void shouldInviteUserAndAcceptInvitation() throws Exception {
        createCommunity();
        Long groupId = communityRepository.findAll().get(0).getId();

        mockMvc.perform(post("/api/communities/{id}/invitations", groupId)
                .with(jwt().jwt(token -> token.subject(OWNER_EMAIL)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"other@community.cl\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.type").value("INVITATION"));

        Long invitationId = accessRepository.findAll().get(0).getId();
        mockMvc.perform(patch(
                "/api/communities/invitations/{id}/accept", invitationId)
                .with(jwt().jwt(token -> token.subject(OTHER_EMAIL))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("APPROVED"));

        assertEquals(2, membershipRepository.countByCommunityId(groupId));
    }

    @Test
    void shouldNotExposeHiddenGroupForJoinRequests() throws Exception {
        mockMvc.perform(post("/api/communities")
                .with(jwt().jwt(token -> token.subject(OWNER_EMAIL)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"Grupo secreto","description":"Solo con invitación","visibility":"HIDDEN"}
                    """))
            .andExpect(status().isCreated());
        Long groupId = communityRepository.findAll().get(0).getId();

        mockMvc.perform(post("/api/communities/{id}/join-requests", groupId)
                .with(jwt().jwt(token -> token.subject(OTHER_EMAIL))))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldAllowRequesterToCancelPendingJoinRequest() throws Exception {
        createPublicCommunity();
        Long groupId = communityRepository.findAll().get(0).getId();

        mockMvc.perform(post("/api/communities/{id}/join-requests", groupId)
                .with(jwt().jwt(token -> token.subject(OTHER_EMAIL))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("PENDING"));

        mockMvc.perform(delete(
                "/api/communities/{id}/join-requests/mine", groupId)
                .with(jwt().jwt(token -> token.subject(OTHER_EMAIL))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(get("/api/communities/{id}/join-requests", groupId)
                .with(jwt().jwt(token -> token.subject(OWNER_EMAIL))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void shouldAllowOwnerToPromoteCommunityMember() throws Exception {
        createCommunity();
        var group = communityRepository.findAll().get(0);
        var member = userRepository.findByEmailIgnoreCase(OTHER_EMAIL).orElseThrow();
        membershipRepository.save(new CommunityMembership(
            group, member, CommunityRole.MEMBER));

        mockMvc.perform(patch(
                "/api/communities/{id}/members/{userId}/role",
                group.getId(), member.getId())
                .with(jwt().jwt(token -> token.subject(OWNER_EMAIL)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"COMMUNITY_MODERATOR\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("COMMUNITY_MODERATOR"));

        assertEquals(CommunityRole.COMMUNITY_MODERATOR,
            membershipRepository.findByCommunityIdAndUserId(
                group.getId(), member.getId()).orElseThrow().getRole());
    }

    @Test
    void shouldRejectRoleChangesFromRegularMembers() throws Exception {
        createCommunity();
        var group = communityRepository.findAll().get(0);
        var member = userRepository.findByEmailIgnoreCase(OTHER_EMAIL).orElseThrow();
        membershipRepository.save(new CommunityMembership(
            group, member, CommunityRole.MEMBER));

        mockMvc.perform(patch(
                "/api/communities/{id}/members/{userId}/role",
                group.getId(), member.getId())
                .with(jwt().jwt(token -> token.subject(OTHER_EMAIL)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"COMMUNITY_MODERATOR\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowMembersToCreateAndReadPosts() throws Exception {
        createCommunity();
        var group = communityRepository.findAll().get(0);
        var member = userRepository.findByEmailIgnoreCase(OTHER_EMAIL).orElseThrow();
        membershipRepository.save(new CommunityMembership(
            group, member, CommunityRole.MEMBER));

        mockMvc.perform(post("/api/communities/{id}/posts", group.getId())
                .with(jwt().jwt(token -> token.subject(OTHER_EMAIL)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Bienvenida\",\"content\":\"Hola comunidad\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("Bienvenida"))
            .andExpect(jsonPath("$.content").value("Hola comunidad"))
            .andExpect(jsonPath("$.editable").value(true));

        mockMvc.perform(post("/api/communities/{id}/posts", group.getId())
                .with(jwt().jwt(token -> token.subject(OTHER_EMAIL)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":null,\"content\":\"Segunda publicación\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").doesNotExist())
            .andExpect(jsonPath("$.content").value("Segunda publicación"));

        mockMvc.perform(get("/api/communities/{id}/posts", group.getId())
                .with(jwt().jwt(token -> token.subject(OWNER_EMAIL))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].authorName").value("Otra persona"))
            .andExpect(jsonPath("$[0].deletable").value(true));
    }

    @Test
    void shouldPreventMembersFromEditingAnotherUsersPost() throws Exception {
        createCommunity();
        var group = communityRepository.findAll().get(0);
        var member = userRepository.findByEmailIgnoreCase(OTHER_EMAIL).orElseThrow();
        membershipRepository.save(new CommunityMembership(
            group, member, CommunityRole.MEMBER));

        mockMvc.perform(post("/api/communities/{id}/posts", group.getId())
                .with(jwt().jwt(token -> token.subject(OWNER_EMAIL)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Aviso\",\"content\":\"Publicación original\"}"))
            .andExpect(status().isCreated());
        Long postId = postRepository.findAll().get(0).getId();

        mockMvc.perform(patch("/api/communities/{id}/posts/{postId}",
                group.getId(), postId)
                .with(jwt().jwt(token -> token.subject(OTHER_EMAIL)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Cambio\",\"content\":\"Intento de edición\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldEnforceCommentPermissionsAndNotifyAuthors() throws Exception {
        createCommunity();
        var group = communityRepository.findAll().get(0);
        var owner = userRepository.findByEmailIgnoreCase(OWNER_EMAIL).orElseThrow();
        var member = userRepository.findByEmailIgnoreCase(OTHER_EMAIL).orElseThrow();

        mockMvc.perform(post("/api/communities/{id}/posts", group.getId())
                .with(jwt().jwt(token -> token.subject(OWNER_EMAIL)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Aviso","content":"Publicación original"}
                    """))
            .andExpect(status().isCreated());
        var postId = postRepository.findAll().get(0).getId();
        var url = "/api/communities/" + group.getId() + "/posts/" + postId + "/comments";
        var ownerJwt = jwt().jwt(token -> token.subject(OWNER_EMAIL));
        var memberJwt = jwt().jwt(token -> token.subject(OTHER_EMAIL));

        mockMvc.perform(get(url).with(memberJwt)).andExpect(status().isNotFound());
        membershipRepository.save(new CommunityMembership(group, member, CommunityRole.MEMBER));
        mockMvc.perform(post(url).with(memberJwt).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"content":"Buen aporte"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.editable").value(true));
        assertEquals(1, notificationRepository.countByRecipientAndReadAtIsNull(owner));
        assertEquals(0, notificationRepository.countByRecipientAndReadAtIsNull(member));

        var commentId = commentRepository.findAllByPostIdOrderByCreatedAtAscIdAsc(postId).get(0).getId();
        var commentUrl = url + "/" + commentId;
        mockMvc.perform(get(url).with(ownerJwt))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].editable").value(false))
            .andExpect(jsonPath("$[0].deletable").value(true));
        mockMvc.perform(patch(commentUrl).with(ownerJwt).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"content":"Cambio ajeno"}
                    """))
            .andExpect(status().isForbidden());
        mockMvc.perform(patch(commentUrl).with(memberJwt).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"content":"Comentario editado"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").value("Comentario editado"));

        mockMvc.perform(post(url).with(ownerJwt).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"content":"Gracias","parentId":%d}
                    """.formatted(commentId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.parentId").value(commentId));
        assertEquals(1, notificationRepository.countByRecipientAndReadAtIsNull(member));
        assertEquals(1, notificationRepository.countByRecipientAndReadAtIsNull(owner));

        var replyId = commentRepository.findAllByPostIdOrderByCreatedAtAscIdAsc(postId).get(1).getId();
        mockMvc.perform(delete(url + "/" + replyId).with(memberJwt))
            .andExpect(status().isForbidden());
        mockMvc.perform(delete(commentUrl).with(ownerJwt)).andExpect(status().isBadRequest());
        mockMvc.perform(delete(commentUrl).with(ownerJwt).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"reason":"  Fuera de las reglas  "}
                    """))
            .andExpect(status().isNoContent());
        assertEquals("Fuera de las reglas", jdbc.sql("""
            SELECT reason FROM community_moderation_log WHERE comment_id = :id
            """).param("id", commentId).query(String.class).single());
        assertEquals(2, notificationRepository.countByRecipientAndReadAtIsNull(member));
        mockMvc.perform(get(url).with(memberJwt))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].content").value("Gracias"))
            .andExpect(jsonPath("$[0].parentId").doesNotExist());
    }
    @Test
    void shouldCountDistinctReactionTypesAndNotifyOnlyOnNewReactions() throws Exception {
        createCommunity();
        var group = communityRepository.findAll().get(0);
        var owner = userRepository.findByEmailIgnoreCase(OWNER_EMAIL).orElseThrow();
        var member = userRepository.findByEmailIgnoreCase(OTHER_EMAIL).orElseThrow();
        var ownerJwt = jwt().jwt(token -> token.subject(OWNER_EMAIL));
        var memberJwt = jwt().jwt(token -> token.subject(OTHER_EMAIL));

        var postsUrl = "/api/communities/" + group.getId() + "/posts";
        mockMvc.perform(post(postsUrl).with(ownerJwt).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Aviso","content":"Publicación original"}
                    """))
            .andExpect(status().isCreated());
        var postId = postRepository.findAll().get(0).getId();
        var reactionUrl = postsUrl + "/" + postId + "/reactions/";

        mockMvc.perform(put(reactionUrl + "LIKE").with(memberJwt)).andExpect(status().isNotFound());
        membershipRepository.save(new CommunityMembership(group, member, CommunityRole.MEMBER));
        mockMvc.perform(put(reactionUrl + "LIKE").with(memberJwt))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].count").value(1))
            .andExpect(jsonPath("$[0].selected").value(true));
        mockMvc.perform(put(reactionUrl + "LIKE").with(memberJwt))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].count").value(1));
        mockMvc.perform(get(postsUrl).with(ownerJwt))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].reactions[0].selected").value(false));
        mockMvc.perform(put(reactionUrl + "LIKE").with(ownerJwt))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].count").value(2));

        mockMvc.perform(put(reactionUrl + "LOVE").with(memberJwt))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));
        mockMvc.perform(delete(reactionUrl + "LOVE").with(ownerJwt))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));
        mockMvc.perform(delete(reactionUrl + "LOVE").with(memberJwt))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));
        assertEquals(2, notificationRepository.countByRecipientAndReadAtIsNull(owner));
        assertEquals(0, notificationRepository.countByRecipientAndReadAtIsNull(member));
        mockMvc.perform(delete(postsUrl + "/" + postId).with(ownerJwt))
            .andExpect(status().isNoContent());
    }
    @Test
    void shouldRequireCommunityRoleAndReasonToModeratePost() throws Exception {
        createCommunity();
        var group = communityRepository.findAll().get(0);
        var member = userRepository.findByEmailIgnoreCase(OTHER_EMAIL).orElseThrow();
        membershipRepository.save(new CommunityMembership(group, member, CommunityRole.MEMBER));
        var ownerJwt = jwt().jwt(token -> token.subject(OWNER_EMAIL));
        var memberJwt = jwt().jwt(token -> token.subject(OTHER_EMAIL));
        var postsUrl = "/api/communities/" + group.getId() + "/posts";

        mockMvc.perform(post(postsUrl).with(memberJwt).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Aviso","content":"Publicación"}
                    """))
            .andExpect(status().isCreated());
        var postId = postRepository.findAll().get(0).getId();
        var url = postsUrl + "/" + postId;
        mockMvc.perform(delete(url).with(ownerJwt)).andExpect(status().isBadRequest());
        mockMvc.perform(get(postsUrl).with(memberJwt))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(delete(url).with(ownerJwt).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"reason":"Contenido fuera de tema"}
                    """))
            .andExpect(status().isNoContent());
        mockMvc.perform(delete(url).with(ownerJwt)).andExpect(status().isNotFound());
        assertEquals(1L, jdbc.sql("""
            SELECT COUNT(*) FROM community_moderation_log WHERE post_id = :id AND comment_id IS NULL
            """).param("id", postId).query(Long.class).single());
        assertEquals(1, notificationRepository.countByRecipientAndReadAtIsNull(member));
    }

    @Test
    void shouldReviewReportsWithoutDuplicatingPendingSubmissions() throws Exception {
        createCommunity();
        var group = communityRepository.findAll().get(0);
        var member = userRepository.findByEmailIgnoreCase(OTHER_EMAIL).orElseThrow();
        var owner = userRepository.findByEmailIgnoreCase(OWNER_EMAIL).orElseThrow();
        var ownerJwt = jwt().jwt(token -> token.subject(OWNER_EMAIL));
        var memberJwt = jwt().jwt(token -> token.subject(OTHER_EMAIL));
        var groupUrl = "/api/communities/" + group.getId();
        var reportsUrl = groupUrl + "/reports";

        mockMvc.perform(post(groupUrl + "/posts").with(ownerJwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Aviso","content":"Publicación original"}
                    """))
            .andExpect(status().isCreated());
        var postId = postRepository.findAll().get(0).getId();
        String report = """
            {"postId":%d,"reason":"Revisar contenido"}
            """.formatted(postId);
        mockMvc.perform(post(reportsUrl).with(memberJwt).contentType(MediaType.APPLICATION_JSON)
                .content(report))
            .andExpect(status().isNotFound());
        membershipRepository.save(new CommunityMembership(group, member, CommunityRole.MEMBER));
        mockMvc.perform(post(reportsUrl).with(ownerJwt).contentType(MediaType.APPLICATION_JSON)
                .content(report))
            .andExpect(status().isBadRequest());
        mockMvc.perform(post(reportsUrl).with(memberJwt).contentType(MediaType.APPLICATION_JSON)
                .content(report))
            .andExpect(status().isCreated());
        mockMvc.perform(post(reportsUrl).with(memberJwt).contentType(MediaType.APPLICATION_JSON)
                .content(report))
            .andExpect(status().isConflict());
        mockMvc.perform(get(reportsUrl).with(memberJwt)).andExpect(status().isForbidden());
        mockMvc.perform(get(reportsUrl).with(ownerJwt)).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].content").value("Publicación original"));
        var reportId = jdbc.sql("SELECT id FROM community_reports WHERE post_id = :id")
            .param("id", postId).query(Long.class).single();
        var dismiss = """
            {"resolution":"DISMISSED","reason":"Revisado"}
            """;
        mockMvc.perform(patch(reportsUrl + "/" + reportId).with(memberJwt)
                .contentType(MediaType.APPLICATION_JSON).content(dismiss))
            .andExpect(status().isForbidden());
        mockMvc.perform(patch(reportsUrl + "/" + reportId).with(ownerJwt)
                .contentType(MediaType.APPLICATION_JSON).content(dismiss))
            .andExpect(status().isNoContent());
        mockMvc.perform(get(reportsUrl).with(ownerJwt)).andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(post(groupUrl + "/posts/" + postId + "/comments").with(memberJwt)
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"content":"Comentario para revisar"}
                    """))
            .andExpect(status().isCreated());
        var commentId = commentRepository.findAllByPostIdOrderByCreatedAtAscIdAsc(postId).get(0).getId();
        mockMvc.perform(post(reportsUrl).with(ownerJwt).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"postId":%d,"commentId":%d,"reason":"Infringe las reglas"}
                    """.formatted(postId, commentId)))
            .andExpect(status().isCreated());
        var commentReport = jdbc.sql("SELECT id FROM community_reports WHERE comment_id = :id")
            .param("id", commentId).query(Long.class).single();
        mockMvc.perform(patch(reportsUrl + "/" + commentReport).with(ownerJwt)
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"resolution":"REMOVED","reason":"Fuera de las reglas"}
                    """))
            .andExpect(status().isNoContent());

        assertEquals(0, commentRepository.findAllByPostIdOrderByCreatedAtAscIdAsc(postId).size());
        assertEquals(1, notificationRepository.countByRecipientAndReadAtIsNull(member));
        assertEquals(owner.getId(), jdbc.sql("""
            SELECT resolved_by FROM community_reports WHERE id = :id AND resolution = 'REMOVED'
            """).param("id", commentReport).query(Long.class).single());
    }
    private void createCommunity() throws Exception {
        mockMvc.perform(post("/api/communities")
                .with(jwt().jwt(token -> token.subject(OWNER_EMAIL)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"Comunidad Angular","description":"Aprendizaje colaborativo","visibility":"PRIVATE"}
                    """))
            .andExpect(status().isCreated());
    }

    private void createPublicCommunity() throws Exception {
        mockMvc.perform(post("/api/communities")
                .with(jwt().jwt(token -> token.subject(OWNER_EMAIL)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"Grupo público","description":"Ingreso mediante solicitud","visibility":"PUBLIC"}
                    """))
            .andExpect(status().isCreated());
    }
}
