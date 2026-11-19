package cl.gestortareas.api.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
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
class CommunityGoalControllerIntegrationTests {
    private static final String OWNER = "goal-owner@community.cl";
    private static final String MEMBER = "goal-member@community.cl";
    private static final String OUTSIDER = "goal-outsider@community.cl";
    @Autowired MockMvc mvc;
    @Autowired JdbcClient jdbc;
    @Autowired AppUserRepository users;
    @Autowired CommunityRepository communities;
    @Autowired CommunityMembershipRepository memberships;
    private Long groupId;
    private Long memberId;
    private Long taskId;
    private String url;

    @BeforeEach
    void setup() throws Exception {
        users.save(new AppUser("Organizadora", OWNER, "hash"));
        var member = users.save(new AppUser("Miembro", MEMBER, "hash"));
        memberId = member.getId();
        users.save(new AppUser("Visitante", OUTSIDER, "hash"));
        groupId = group();
        memberships.saveAndFlush(new CommunityMembership(communities.findById(groupId).orElseThrow(), member, CommunityRole.MEMBER));
        taskId = task(groupId);
        url = "/api/communities/" + groupId + "/goals";
    }

    @Test
    void computesProgressFromTasksAcrossGoalsAndDeduplicatesLinks() throws Exception {
        create("Meta uno", "[" + taskId + "," + taskId + "]")
            .andExpect(jsonPath("$.goals[0].totalTasks").value(1))
            .andExpect(jsonPath("$.goals[0].progress").value(0));
        create("Meta dos", "[" + taskId + "]");
        mvc.perform(patch("/api/communities/" + groupId + "/tasks/" + taskId + "/status")
            .with(jwt().jwt(t -> t.subject(MEMBER))).contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\":\"COMPLETED\"}")).andExpect(status().isNoContent());
        mvc.perform(get(url).with(jwt().jwt(t -> t.subject(MEMBER))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.canManage").value(false))
            .andExpect(jsonPath("$.tasks[0].canChangeStatus").value(true))
            .andExpect(jsonPath("$.goals[0].progress").value(100))
            .andExpect(jsonPath("$.goals[1].completedTasks").value(1));
        create("Sin tareas", "[]").andExpect(jsonPath("$.goals[0].progress").value(0));
    }

    @Test
    void enforcesMembershipManagementAndGroupBoundaries() throws Exception {
        create("Original", "[" + taskId + "]");
        Long id = goalId();
        mvc.perform(get(url).with(jwt().jwt(t -> t.subject(OUTSIDER)))).andExpect(status().isNotFound());
        mvc.perform(post(url).with(jwt().jwt(t -> t.subject(MEMBER))).contentType(MediaType.APPLICATION_JSON)
            .content(body("Sin permiso", "[]"))).andExpect(status().isForbidden());
        mvc.perform(put(url + "/" + id).with(jwt().jwt(t -> t.subject(MEMBER))).contentType(MediaType.APPLICATION_JSON)
            .content(body("Sin permiso", "[]"))).andExpect(status().isForbidden());
        mvc.perform(delete(url + "/" + id).with(jwt().jwt(t -> t.subject(MEMBER)))).andExpect(status().isForbidden());
        Long foreignTask = task(group());
        mvc.perform(put(url + "/" + id).with(jwt().jwt(t -> t.subject(OWNER))).contentType(MediaType.APPLICATION_JSON)
            .content(body("Cambio inválido", "[" + foreignTask + "]"))).andExpect(status().isBadRequest());
        mvc.perform(get(url).with(jwt().jwt(t -> t.subject(OWNER))))
            .andExpect(jsonPath("$.goals[0].title").value("Original"))
            .andExpect(jsonPath("$.goals[0].taskIds[0]").value(taskId));
    }

    @Test
    void editsAndDeletesGoalsWithoutDeletingTasksAndRemovesDeletedTaskLinks() throws Exception {
        create("Original", "[" + taskId + "]");
        Long id = goalId();
        mvc.perform(put(url + "/" + id).with(jwt().jwt(t -> t.subject(OWNER))).contentType(MediaType.APPLICATION_JSON)
            .content(body("Editado", "[]"))).andExpect(status().isOk())
            .andExpect(jsonPath("$.goals[0].title").value("Editado"))
            .andExpect(jsonPath("$.goals[0].totalTasks").value(0));
        mvc.perform(delete(url + "/" + id).with(jwt().jwt(t -> t.subject(OWNER))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.goals").isEmpty());
        assertEquals(1L, jdbc.sql("SELECT COUNT(*) FROM community_tasks WHERE id = :id").param("id", taskId).query(Long.class).single());
        create("Cascade", "[" + taskId + "]");
        jdbc.sql("DELETE FROM community_tasks WHERE id = :id").param("id", taskId).update();
        mvc.perform(get(url).with(jwt().jwt(t -> t.subject(OWNER))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.goals[0].totalTasks").value(0));
    }

    private org.springframework.test.web.servlet.ResultActions create(String title, String ids) throws Exception {
        return mvc.perform(post(url).with(jwt().jwt(t -> t.subject(OWNER))).contentType(MediaType.APPLICATION_JSON)
            .content(body(title, ids))).andExpect(status().isCreated());
    }
    private String body(String title, String ids) {
        return "{\"title\":\"" + title + "\",\"description\":\"Descripción\",\"dueDate\":null,\"taskIds\":" + ids + "}";
    }
    private Long goalId() { return jdbc.sql("SELECT MAX(id) FROM community_goals").query(Long.class).single(); }
    private Long task(Long group) {
        jdbc.sql("INSERT INTO community_tasks (community_id, description, assignee_id) VALUES (:group, 'Preparar evento', :user)")
            .param("group", group).param("user", memberId).update();
        return jdbc.sql("SELECT MAX(id) FROM community_tasks").query(Long.class).single();
    }
    private Long group() throws Exception {
        mvc.perform(post("/api/communities").with(jwt().jwt(t -> t.subject(OWNER))).contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Objetivos\",\"description\":\"Pruebas\",\"visibility\":\"PRIVATE\"}"))
            .andExpect(status().isCreated());
        return jdbc.sql("SELECT MAX(id) FROM communities").query(Long.class).single();
    }
}
