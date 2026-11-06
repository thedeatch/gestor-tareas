package cl.gestortareas.api.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import cl.gestortareas.api.repository.AppUserRepository;
import cl.gestortareas.api.repository.CommunityAccessRequestRepository;
import cl.gestortareas.api.repository.CommunityMembershipRepository;
import cl.gestortareas.api.repository.CommunityRepository;
import cl.gestortareas.api.repository.NotificationRepository;

@SpringBootTest
@AutoConfigureMockMvc
class CommunityControllerIntegrationTests {
    private static final String OWNER_EMAIL = "owner@community.cl";
    private static final String OTHER_EMAIL = "other@community.cl";

    @Autowired MockMvc mockMvc;
    @Autowired CommunityRepository communityRepository;
    @Autowired CommunityMembershipRepository membershipRepository;
    @Autowired CommunityAccessRequestRepository accessRepository;
    @Autowired NotificationRepository notificationRepository;
    @Autowired AppUserRepository userRepository;

    @BeforeEach
    void cleanDatabase() {
        notificationRepository.deleteAll();
        accessRepository.deleteAll();
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
