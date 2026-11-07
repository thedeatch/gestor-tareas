package cl.gestortareas.api.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
    @Test
    void shouldKeepGuidelinesForMembersAndRestrictEditingToOwnersAndAdmins() throws Exception {
        createCommunity();
        var group = communityRepository.findAll().get(0);
        var member = userRepository.findByEmailIgnoreCase(OTHER_EMAIL).orElseThrow();
        var url = "/api/communities/" + group.getId();
        var ownerJwt = jwt().jwt(token -> token.subject(OWNER_EMAIL));
        var memberJwt = jwt().jwt(token -> token.subject(OTHER_EMAIL));
        String guidelines = """
            {"extendedDescription":"  Detalles del grupo  ","rules":"  Respetar a los miembros  "}
            """;

        mockMvc.perform(get(url).with(ownerJwt)).andExpect(status().isOk())
            .andExpect(jsonPath("$.extendedDescription").value(""))
            .andExpect(jsonPath("$.rules").value(""));
        mockMvc.perform(patch(url + "/guidelines").with(memberJwt)
                .contentType(MediaType.APPLICATION_JSON).content(guidelines))
            .andExpect(status().isNotFound());
        var membership = membershipRepository.save(new CommunityMembership(group, member, CommunityRole.MEMBER));
        mockMvc.perform(patch(url + "/guidelines").with(memberJwt)
                .contentType(MediaType.APPLICATION_JSON).content(guidelines))
            .andExpect(status().isForbidden());

        mockMvc.perform(patch(url + "/guidelines").with(ownerJwt)
                .contentType(MediaType.APPLICATION_JSON).content(guidelines))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.description").value("Aprendizaje colaborativo"))
            .andExpect(jsonPath("$.extendedDescription").value("Detalles del grupo"))
            .andExpect(jsonPath("$.rules").value("Respetar a los miembros"));
        mockMvc.perform(get(url).with(memberJwt)).andExpect(status().isOk())
            .andExpect(jsonPath("$.rules").value("Respetar a los miembros"));
        mockMvc.perform(get("/api/communities").with(memberJwt)).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].extendedDescription").doesNotExist())
            .andExpect(jsonPath("$[0].rules").doesNotExist());

        membershipRepository.delete(membership);
        membershipRepository.save(new CommunityMembership(group, member, CommunityRole.COMMUNITY_MODERATOR));
        mockMvc.perform(patch(url + "/guidelines").with(memberJwt)
                .contentType(MediaType.APPLICATION_JSON).content(guidelines))
            .andExpect(status().isForbidden());

        mockMvc.perform(patch(url + "/members/" + member.getId() + "/role").with(ownerJwt)
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"role":"COMMUNITY_ADMIN"}
                    """))
            .andExpect(status().isOk());
        mockMvc.perform(patch(url + "/guidelines").with(memberJwt)
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"extendedDescription":"","rules":""}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rules").value(""));
    }
    @Test
    void shouldAllowGroupEventRegistrationOncePerMember() throws Exception {
        createCommunity();
        var group = communityRepository.findAll().get(0);
        var member = userRepository.findByEmailIgnoreCase(OTHER_EMAIL).orElseThrow();
        var ownerJwt = jwt().jwt(token -> token.subject(OWNER_EMAIL));
        var memberJwt = jwt().jwt(token -> token.subject(OTHER_EMAIL));
        var events = "/api/communities/" + group.getId() + "/events";
        String payload = """
            {"title":"Encuentro","description":"Actividad del grupo","startsAt":"%s"}
            """;
        var now = java.time.OffsetDateTime.now();
        String future = payload.formatted(now.plusDays(2));

        mockMvc.perform(get(events).with(memberJwt)).andExpect(status().isNotFound());
        membershipRepository.save(new CommunityMembership(group, member, CommunityRole.MEMBER));
        mockMvc.perform(post(events).with(memberJwt).contentType(MediaType.APPLICATION_JSON)
                .content(future)).andExpect(status().isForbidden());
        mockMvc.perform(post(events).with(ownerJwt).contentType(MediaType.APPLICATION_JSON)
                .content(payload.formatted(now.minusDays(1)))).andExpect(status().isBadRequest());
        mockMvc.perform(post(events).with(ownerJwt).contentType(MediaType.APPLICATION_JSON)
                .content(future)).andExpect(status().isCreated());
        var eventId = jdbc.sql("SELECT id FROM community_events WHERE community_id = :group")
            .param("group", group.getId()).query(Long.class).single();
        var registration = events + "/" + eventId + "/registration";

        mockMvc.perform(put(registration).with(memberJwt)).andExpect(status().isNoContent());
        mockMvc.perform(put(registration).with(memberJwt)).andExpect(status().isNoContent());
        mockMvc.perform(get(events).with(memberJwt)).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].participantCount").value(1))
            .andExpect(jsonPath("$[0].registered").value(true));
        mockMvc.perform(get(events).with(ownerJwt)).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].registered").value(false));
        mockMvc.perform(delete(registration).with(memberJwt)).andExpect(status().isNoContent());
        mockMvc.perform(get(events).with(memberJwt)).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].participantCount").value(0))
            .andExpect(jsonPath("$[0].registered").value(false));

        jdbc.sql("UPDATE community_events SET starts_at = :past WHERE id = :id")
            .param("past", now.minusDays(1)).param("id", eventId).update();
        mockMvc.perform(put(registration).with(memberJwt)).andExpect(status().isBadRequest());
    }
    @Test
    void shouldProtectAlbumUploadsAndImageDownloadsByGroupMembership() throws Exception {
        createCommunity();
        var group = communityRepository.findAll().get(0);
        var ownerJwt = jwt().jwt(token -> token.subject(OWNER_EMAIL));
        var memberJwt = jwt().jwt(token -> token.subject(OTHER_EMAIL));
        var albums = "/api/communities/" + group.getId() + "/albums";
        String payload = """
            {"title":"Encuentro","description":"Fotos del grupo"}
            """;

        mockMvc.perform(get(albums).with(memberJwt)).andExpect(status().isNotFound());
        mockMvc.perform(post(albums).with(ownerJwt).contentType(MediaType.APPLICATION_JSON)
                .content(payload)).andExpect(status().isCreated());
        var albumId = jdbc.sql("SELECT id FROM community_albums WHERE community_id = :id")
            .param("id", group.getId()).query(Long.class).single();
        var images = albums + "/" + albumId + "/images";
        byte[] png = java.util.Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aN1sAAAAASUVORK5CYII=");
        var image = new org.springframework.mock.web.MockMultipartFile("file", "foto.png", "image/png", png);
        var fake = new org.springframework.mock.web.MockMultipartFile(
            "file", "falso.png", "image/png", new byte[] {1, 2, 3});

        mockMvc.perform(multipart(images).file(fake).with(ownerJwt))
            .andExpect(status().isUnsupportedMediaType());
        mockMvc.perform(multipart(images).file(image).with(ownerJwt))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.name").value("foto.png"));
        var fileId = jdbc.sql("SELECT file_id FROM community_album_images WHERE album_id = :id")
            .param("id", albumId).query(Long.class).single();

        mockMvc.perform(get(images + "/" + fileId).with(memberJwt)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/files/" + fileId).with(ownerJwt)).andExpect(status().isNotFound());
        var member = userRepository.findByEmailIgnoreCase(OTHER_EMAIL).orElseThrow();
        membershipRepository.save(new CommunityMembership(group, member, CommunityRole.MEMBER));
        mockMvc.perform(get(albums).with(memberJwt)).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].title").value("Encuentro"));
        mockMvc.perform(get(images).with(memberJwt)).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(fileId));
        mockMvc.perform(get(images + "/" + fileId).with(memberJwt)).andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.IMAGE_PNG)).andExpect(content().bytes(png));
        mockMvc.perform(multipart(images).file(image).with(memberJwt)).andExpect(status().isForbidden());
        mockMvc.perform(post(albums).with(memberJwt).contentType(MediaType.APPLICATION_JSON)
                .content(payload)).andExpect(status().isForbidden());
    }
    @Test
    void shouldDeliverNewChatMessagesToWaitingGroupMembers() throws Exception {
        createCommunity();
        var group = communityRepository.findAll().get(0);
        var member = userRepository.findByEmailIgnoreCase(OTHER_EMAIL).orElseThrow();
        var ownerJwt = jwt().jwt(token -> token.subject(OWNER_EMAIL));
        var memberJwt = jwt().jwt(token -> token.subject(OTHER_EMAIL));
        var chat = "/api/communities/" + group.getId() + "/chat";
        String message = """
            {"content":"Hola, grupo"}
            """;

        mockMvc.perform(get(chat).with(memberJwt)).andExpect(status().isNotFound());
        mockMvc.perform(post(chat).with(memberJwt).contentType(MediaType.APPLICATION_JSON).content(message))
            .andExpect(status().isNotFound());
        membershipRepository.save(new CommunityMembership(group, member, CommunityRole.MEMBER));

        var waiting = mockMvc.perform(get(chat).param("after", "0").with(memberJwt))
            .andExpect(request().asyncStarted()).andReturn();
        mockMvc.perform(post(chat).with(ownerJwt).contentType(MediaType.APPLICATION_JSON).content(message))
            .andExpect(status().isCreated());
        waiting.getAsyncResult(5000);
        mockMvc.perform(asyncDispatch(waiting)).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].content").value("Hola, grupo"))
            .andExpect(jsonPath("$[0].authorName").value("Propietaria"));

        var history = mockMvc.perform(get(chat).with(memberJwt))
            .andExpect(request().asyncStarted()).andReturn();
        mockMvc.perform(asyncDispatch(history)).andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(post(chat).with(ownerJwt).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"content":"   "}
                    """))
            .andExpect(status().isBadRequest());
    }
    @Test
    void shouldShowOnlyDatedActivityFromTheMembersGroupInReverseOrder() throws Exception {
        createCommunity();
        var group = communityRepository.findAll().get(0);
        var ownerJwt = jwt().jwt(token -> token.subject(OWNER_EMAIL));
        var outsiderJwt = jwt().jwt(token -> token.subject(OTHER_EMAIL));
        var url = "/api/communities/" + group.getId();

        mockMvc.perform(post(url + "/posts").with(ownerJwt).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Publicación inicial","content":"Contenido"}
                    """))
            .andExpect(status().isCreated());
        var postId = postRepository.findAll().get(0).getId();
        mockMvc.perform(post(url + "/albums").with(ownerJwt).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Fotos nuevas","description":""}
                    """))
            .andExpect(status().isCreated());
        String startsAt = java.time.OffsetDateTime.now().plusDays(1).toString();
        mockMvc.perform(post(url + "/events").with(ownerJwt).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Encuentro","description":"Actividad","startsAt":"%s"}
                    """.formatted(startsAt)))
            .andExpect(status().isCreated());

        jdbc.sql("""
            UPDATE community_posts SET created_at = :date WHERE id = :id
            """).param("date", java.time.OffsetDateTime.parse("2025-01-01T00:00:00Z"))
            .param("id", postId).update();
        jdbc.sql("""
            UPDATE community_albums SET created_at = :date WHERE community_id = :id
            """).param("date", java.time.OffsetDateTime.parse("2025-01-02T00:00:00Z"))
            .param("id", group.getId()).update();
        jdbc.sql("""
            INSERT INTO community_albums (community_id, title, description, created_at)
            VALUES (:id, 'Álbum antiguo', '', NULL)
            """).param("id", group.getId()).update();
        createPublicCommunity();
        var otherGroup = communityRepository.findAll().stream()
            .filter(item -> !item.getId().equals(group.getId())).findFirst().orElseThrow();
        jdbc.sql("""
            INSERT INTO community_albums (community_id, title, description)
            VALUES (:id, 'De otro grupo', '')
            """).param("id", otherGroup.getId()).update();

        mockMvc.perform(get(url + "/activity").with(outsiderJwt)).andExpect(status().isNotFound());
        mockMvc.perform(get(url + "/activity").with(ownerJwt)).andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].type").value("EVENT"))
            .andExpect(jsonPath("$[1].type").value("ALBUM"))
            .andExpect(jsonPath("$[2].type").value("POST"))
            .andExpect(jsonPath("$[2].resourceId").value(postId));
        mockMvc.perform(delete(url + "/posts/" + postId).with(ownerJwt)).andExpect(status().isNoContent());
        mockMvc.perform(get(url + "/activity").with(ownerJwt)).andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));
    }
    @Test
    void shouldHideChatAndDirectInvitationsBetweenBlockedUsers() throws Exception {
        createCommunity();
        var group = communityRepository.findAll().get(0);
        var owner = userRepository.findByEmailIgnoreCase(OWNER_EMAIL).orElseThrow();
        var member = userRepository.findByEmailIgnoreCase(OTHER_EMAIL).orElseThrow();
        var ownerJwt = jwt().jwt(token -> token.subject(OWNER_EMAIL));
        var memberJwt = jwt().jwt(token -> token.subject(OTHER_EMAIL));
        var groupUrl = "/api/communities/" + group.getId();
        var blocks = "/api/users/me/blocks";
        String invitation = """
            {"email":"%s"}
            """.formatted(OTHER_EMAIL);

        mockMvc.perform(post(groupUrl + "/invitations").with(ownerJwt)
                .contentType(MediaType.APPLICATION_JSON).content(invitation))
            .andExpect(status().isCreated());
        mockMvc.perform(put(blocks + "/" + member.getId()).with(memberJwt))
            .andExpect(status().isBadRequest());
        mockMvc.perform(put(blocks + "/" + owner.getId()).with(memberJwt))
            .andExpect(status().isNoContent());
        mockMvc.perform(put(blocks + "/" + owner.getId()).with(memberJwt))
            .andExpect(status().isNoContent());
        mockMvc.perform(get(blocks).with(memberJwt)).andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(owner.getId()));
        mockMvc.perform(get(blocks).with(ownerJwt)).andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/api/communities/invitations").with(memberJwt)).andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
        var invitationId = accessRepository.findAll().get(0).getId();
        mockMvc.perform(patch("/api/communities/invitations/" + invitationId + "/accept").with(memberJwt))
            .andExpect(status().isForbidden());
        mockMvc.perform(post(groupUrl + "/invitations").with(ownerJwt)
                .contentType(MediaType.APPLICATION_JSON).content(invitation))
            .andExpect(status().isForbidden());

        membershipRepository.save(new CommunityMembership(group, member, CommunityRole.MEMBER));
        mockMvc.perform(post(groupUrl + "/posts").with(ownerJwt).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Aviso","content":"Publicación compartida"}
                    """))
            .andExpect(status().isCreated());
        var postId = postRepository.findAll().get(0).getId();
        mockMvc.perform(put(groupUrl + "/posts/" + postId + "/reactions/LIKE").with(memberJwt))
            .andExpect(status().isOk());
        assertEquals(0, notificationRepository.countByRecipientAndReadAtIsNull(owner));
        var chat = groupUrl + "/chat";
        mockMvc.perform(post(chat).with(ownerJwt).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"content":"Mensaje del propietario"}
                    """))
            .andExpect(status().isCreated());
        mockMvc.perform(post(chat).with(memberJwt).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"content":"Mensaje del miembro"}
                    """))
            .andExpect(status().isCreated());
        var memberHistory = mockMvc.perform(get(chat).with(memberJwt))
            .andExpect(request().asyncStarted()).andReturn();
        mockMvc.perform(asyncDispatch(memberHistory)).andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].content").value("Mensaje del miembro"));
        var ownerHistory = mockMvc.perform(get(chat).with(ownerJwt))
            .andExpect(request().asyncStarted()).andReturn();
        mockMvc.perform(asyncDispatch(ownerHistory)).andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].content").value("Mensaje del propietario"));

        mockMvc.perform(delete(blocks + "/" + owner.getId()).with(memberJwt))
            .andExpect(status().isNoContent());
        var restored = mockMvc.perform(get(chat).with(memberJwt))
            .andExpect(request().asyncStarted()).andReturn();
        mockMvc.perform(asyncDispatch(restored)).andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));
    }
    @Test
    void shouldRankMembersAndAwardBadgesForTheSelectedMonth() throws Exception {
        createCommunity();
        var group = communityRepository.findAll().get(0);
        var member = userRepository.findByEmailIgnoreCase(OTHER_EMAIL).orElseThrow();
        var ownerJwt = jwt().jwt(token -> token.subject(OWNER_EMAIL));
        var memberJwt = jwt().jwt(token -> token.subject(OTHER_EMAIL));
        var groupUrl = "/api/communities/" + group.getId();
        var ranking = groupUrl + "/ranking";

        mockMvc.perform(get(ranking).with(memberJwt)).andExpect(status().isNotFound());
        membershipRepository.save(new CommunityMembership(group, member, CommunityRole.MEMBER));
        mockMvc.perform(get(ranking).with(memberJwt)).andExpect(status().isOk())
            .andExpect(jsonPath("$.leaders.length()").value(0))
            .andExpect(jsonPath("$.viewer.points").value(0));

        mockMvc.perform(post(groupUrl + "/posts").with(ownerJwt).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Aviso","content":"Publicación inicial"}
                    """))
            .andExpect(status().isCreated());
        var postId = postRepository.findAll().get(0).getId();
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post(groupUrl + "/posts/" + postId + "/comments").with(memberJwt)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"content":"Comentario de la temporada"}
                        """))
                .andExpect(status().isCreated());
        }

        mockMvc.perform(get(ranking).with(ownerJwt)).andExpect(status().isOk())
            .andExpect(jsonPath("$.leaders[0].name").value("Otra persona"))
            .andExpect(jsonPath("$.leaders[0].points").value(10))
            .andExpect(jsonPath("$.leaders[0].badges[0]").value("CONVERSATION"))
            .andExpect(jsonPath("$.viewer.position").value(2))
            .andExpect(jsonPath("$.viewer.points").value(5))
            .andExpect(jsonPath("$.viewer.badges[0]").value("FIRST_POST"));
        mockMvc.perform(get(ranking).param("season", "invalid").with(ownerJwt))
            .andExpect(status().isBadRequest());

        var previous = java.time.YearMonth.now(java.time.ZoneOffset.UTC).minusMonths(1);
        var date = previous.atDay(1).atStartOfDay().atOffset(java.time.ZoneOffset.UTC);
        jdbc.sql("UPDATE communities SET created_at = :date WHERE id = :id")
            .param("date", date).param("id", group.getId()).update();
        jdbc.sql("UPDATE community_posts SET created_at = :date WHERE id = :id")
            .param("date", date).param("id", postId).update();
        mockMvc.perform(get(ranking).param("season", previous.toString()).with(ownerJwt))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.season").value(previous.toString()))
            .andExpect(jsonPath("$.leaders.length()").value(1))
            .andExpect(jsonPath("$.viewer.points").value(5));
    }
    @Test
    void shouldCountOnlyGroupActivityAndDistinctEventParticipantsInTheSelectedPeriod() throws Exception {
        createCommunity();
        var group = communityRepository.findAll().get(0);
        var owner = userRepository.findByEmailIgnoreCase(OWNER_EMAIL).orElseThrow();
        var member = userRepository.findByEmailIgnoreCase(OTHER_EMAIL).orElseThrow();
        var ownerJwt = jwt().jwt(token -> token.subject(OWNER_EMAIL));
        var memberJwt = jwt().jwt(token -> token.subject(OTHER_EMAIL));
        var groupUrl = "/api/communities/" + group.getId();
        var metrics = groupUrl + "/metrics";

        mockMvc.perform(get(metrics).with(memberJwt)).andExpect(status().isNotFound());
        mockMvc.perform(get(metrics).param("days", "15").with(ownerJwt)).andExpect(status().isBadRequest());
        membershipRepository.save(new CommunityMembership(group, member, CommunityRole.MEMBER));
        mockMvc.perform(post(groupUrl + "/posts").with(ownerJwt).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Aviso","content":"Publicación inicial"}
                    """))
            .andExpect(status().isCreated());
        var postId = postRepository.findAll().get(0).getId();
        mockMvc.perform(post(groupUrl + "/posts/" + postId + "/comments").with(memberJwt)
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"content":"Comentario reciente"}
                    """))
            .andExpect(status().isCreated());

        createPublicCommunity();
        var otherGroup = communityRepository.findAll().stream()
            .filter(item -> !item.getId().equals(group.getId())).findFirst().orElseThrow();
        var now = java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC);
        jdbc.sql("""
            INSERT INTO community_events (community_id, title, description, starts_at)
            VALUES (:group, 'Primero', '', :recent), (:group, 'Segundo', '', :recent),
                   (:group, 'Anterior', '', :old), (:group, 'Futuro', '', :future),
                   (:other, 'Otro grupo', '', :recent)
            """).param("group", group.getId()).param("other", otherGroup.getId())
            .param("recent", now.minusDays(2)).param("old", now.minusDays(15))
            .param("future", now.plusDays(2)).update();
        jdbc.sql("""
            INSERT INTO community_event_registrations (event_id, user_id)
            SELECT id, :user FROM community_events WHERE community_id = :group
            """).param("user", owner.getId()).param("group", group.getId()).update();

        mockMvc.perform(get(metrics).param("days", "7").with(memberJwt)).andExpect(status().isOk())
            .andExpect(jsonPath("$.days").value(7))
            .andExpect(jsonPath("$.counts.members").value(2))
            .andExpect(jsonPath("$.counts.newMembers").value(2))
            .andExpect(jsonPath("$.counts.posts").value(1))
            .andExpect(jsonPath("$.counts.comments").value(1))
            .andExpect(jsonPath("$.counts.events").value(2))
            .andExpect(jsonPath("$.counts.registrations").value(2))
            .andExpect(jsonPath("$.counts.participants").value(1));
        mockMvc.perform(get(metrics).with(memberJwt)).andExpect(status().isOk())
            .andExpect(jsonPath("$.days").value(30))
            .andExpect(jsonPath("$.counts.events").value(3))
            .andExpect(jsonPath("$.counts.registrations").value(3))
            .andExpect(jsonPath("$.counts.participants").value(1));
    }
    @Test
    void shouldDeliverPrivateMessagesOnlyToParticipantsAndRespectBlocks() throws Exception {
        var sender = userRepository.findByEmailIgnoreCase(OWNER_EMAIL).orElseThrow();
        var recipient = userRepository.findByEmailIgnoreCase(OTHER_EMAIL).orElseThrow();
        String outsiderEmail = "outsider@community.cl";
        userRepository.save(new AppUser("Otra cuenta", outsiderEmail, "hash"));
        var senderJwt = jwt().jwt(token -> token.subject(OWNER_EMAIL));
        var recipientJwt = jwt().jwt(token -> token.subject(OTHER_EMAIL));
        var outsiderJwt = jwt().jwt(token -> token.subject(outsiderEmail));
        var inbox = "/api/private-chat";
        var recipientChat = inbox + "/" + sender.getId();
        var senderChat = inbox + "/" + recipient.getId();

        var waiting = mockMvc.perform(get(recipientChat).param("after", "0").with(recipientJwt))
            .andExpect(request().asyncStarted()).andReturn();
        mockMvc.perform(post(senderChat).with(senderJwt).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"content":"Hola en privado"}
                    """))
            .andExpect(status().isCreated());
        waiting.getAsyncResult(5000);
        mockMvc.perform(asyncDispatch(waiting)).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].content").value("Hola en privado"));
        mockMvc.perform(get(inbox).with(recipientJwt)).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].userId").value(sender.getId()));
        mockMvc.perform(get(inbox).with(outsiderJwt)).andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
        var outsider = mockMvc.perform(get(recipientChat).with(outsiderJwt))
            .andExpect(request().asyncStarted()).andReturn();
        mockMvc.perform(asyncDispatch(outsider)).andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));

        var blockUrl = "/api/users/me/blocks/" + sender.getId();
        mockMvc.perform(put(blockUrl).with(recipientJwt)).andExpect(status().isNoContent());
        mockMvc.perform(get(inbox).with(senderJwt)).andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get(recipientChat).with(recipientJwt)).andExpect(status().isForbidden());
        mockMvc.perform(post(senderChat).with(senderJwt).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"content":"No disponible"}
                    """))
            .andExpect(status().isForbidden());
        mockMvc.perform(delete(blockUrl).with(recipientJwt)).andExpect(status().isNoContent());
        var restored = mockMvc.perform(get(recipientChat).with(recipientJwt))
            .andExpect(request().asyncStarted()).andReturn();
        mockMvc.perform(asyncDispatch(restored)).andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));
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
