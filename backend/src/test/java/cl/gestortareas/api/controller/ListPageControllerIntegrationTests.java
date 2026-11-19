package cl.gestortareas.api.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import cl.gestortareas.api.model.*;
import cl.gestortareas.api.repository.*;

@SpringBootTest @AutoConfigureMockMvc @Transactional
class ListPageControllerIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired JdbcClient jdbc;
    @Autowired AppUserRepository users;
    @Autowired CommunityRepository groups;
    @Autowired CommunityMembershipRepository members;
    @Autowired TaskRepository tasks;
    private final String email = "pages-owner@test.cl", memberEmail = "pages-member@test.cl", outsider = "pages-other@test.cl";
    private Long group, member, firstPost, firstEvent, firstAlbum, firstTask;
    private String base;

    @BeforeEach void setup() {
        var owner = users.saveAndFlush(new AppUser("Organizadora", email, "hash"));
        var viewer = users.saveAndFlush(new AppUser("Miembro", memberEmail, "hash")); member = viewer.getId();
        var other = users.saveAndFlush(new AppUser("Visitante", outsider, "hash"));
        var community = groups.saveAndFlush(new Community("Páginas", "pages-integration", "Pruebas", CommunityVisibility.PRIVATE, owner));
        group = community.getId(); base = "/api/communities/" + group;
        members.saveAndFlush(new CommunityMembership(community, owner, CommunityRole.OWNER));
        members.saveAndFlush(new CommunityMembership(community, viewer, CommunityRole.MEMBER));
        for (int i = 0; i < 45; i++) {
            var postKey = new GeneratedKeyHolder();
            jdbc.sql("INSERT INTO community_posts (community_id, author_user_id, content, created_at, updated_at) VALUES (:groupId, :owner, :text, :date, :date)")
                .param("groupId", group).param("owner", owner.getId()).param("text", "Publicación " + i)
                .param("date", OffsetDateTime.parse("2026-01-01T00:00:00Z").plusMinutes(i)).update(postKey, "id");
            var eventKey = new GeneratedKeyHolder();
            jdbc.sql("INSERT INTO community_events (community_id, title, description, starts_at) VALUES (:groupId, :title, '', :starts)")
                .param("groupId", group).param("title", "Evento " + i).param("starts", OffsetDateTime.now().plusDays(i + 1)).update(eventKey, "id");
            var taskKey = new GeneratedKeyHolder();
            jdbc.sql("INSERT INTO community_tasks (community_id, description, status, priority, due_date, assignee_id) VALUES (:groupId, :description, 'PENDING', 'HIGH', :due, :assignee)")
                .param("groupId", group).param("description", i == 0 ? "Reunión backend antigua" : "Tarea " + i)
                .param("due", LocalDate.of(2026, 11, 25).plusDays(i)).param("assignee", member).update(taskKey, "id");
            var task = new Task(i == 0 ? "Reunión antigua" : "Personal " + i, false); task.setOwner(owner);
            task.updateDetails(task.getDescription(), Task.Priority.HIGH, Task.Status.PENDING, LocalDate.of(2026, 11, 25).plusDays(i), List.of("Backend"));
            tasks.saveAndFlush(task);
            if (i == 0) { firstPost = postKey.getKeyAs(Long.class); firstEvent = eventKey.getKeyAs(Long.class); firstTask = taskKey.getKeyAs(Long.class); }
        }
        var privateTask = new Task("No compartir", false); privateTask.setOwner(other); tasks.saveAndFlush(privateTask);
        for (int i = 0; i < 25; i++) {
            var key = new GeneratedKeyHolder();
            jdbc.sql("INSERT INTO community_albums (community_id, title, description) VALUES (:groupId, :title, '')")
                .param("groupId", group).param("title", "Álbum " + i).update(key, "id");
            if (i == 0) firstAlbum = key.getKeyAs(Long.class);
        }
    }

    @Test void pagesPostsAlbumsAndEventsAndLocatesOldDeepLinks() throws Exception {
        mvc.perform(get(base + "/posts/page").with(jwt().jwt(t -> t.subject(memberEmail))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(45)).andExpect(jsonPath("$.items.length()").value(20))
            .andExpect(jsonPath("$.items[0].content").value("Publicación 44"));
        mvc.perform(get(base + "/posts/page").param("target", firstPost.toString()).with(jwt().jwt(t -> t.subject(memberEmail))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.page").value(2)).andExpect(jsonPath("$.items.length()").value(5));
        mvc.perform(get(base + "/albums/page").param("target", firstAlbum.toString()).with(jwt().jwt(t -> t.subject(email))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.page").value(2)).andExpect(jsonPath("$.size").value(12))
            .andExpect(jsonPath("$.items[0].title").value("Álbum 0"));
        mvc.perform(get(base + "/events/page").param("page", "2").with(jwt().jwt(t -> t.subject(email))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(5)).andExpect(jsonPath("$.items[0].title").value("Evento 40"));
        mvc.perform(get(base + "/events/page").param("target", firstEvent.toString()).with(jwt().jwt(t -> t.subject(email))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.page").value(0));
    }

    @Test void taskFiltersSearchTheWholeCollectionAndKeepOwnership() throws Exception {
        for (String url : new String[] {"/api/tasks/page", base + "/tasks/page"}) {
            mvc.perform(get(url).param("q", "reunion backend").param("status", "PENDING").param("priority", "HIGH")
                .param("due", "WEEK").param("today", "2026-11-19").with(jwt().jwt(t -> t.subject(email))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.allTotal").value(45));
            mvc.perform(get(url).param("q", "%").with(jwt().jwt(t -> t.subject(email))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
            mvc.perform(get(url).param("status", "INVALID").with(jwt().jwt(t -> t.subject(email)))).andExpect(status().isBadRequest());
        }
        mvc.perform(get(base + "/tasks/page").param("target", firstTask.toString()).with(jwt().jwt(t -> t.subject(memberEmail))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.page").value(2)).andExpect(jsonPath("$.items[4].canChangeStatus").value(true));
        mvc.perform(get(base + "/tasks/page").param("assignee", "NONE").with(jwt().jwt(t -> t.subject(memberEmail))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
        mvc.perform(get("/api/tasks/page").with(jwt().jwt(t -> t.subject(outsider))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.items[0].description").value("No compartir"));
    }

    @Test void enforcesMembershipAndValidatesPagesAndTargets() throws Exception {
        mvc.perform(get(base + "/tasks/assignees").with(jwt().jwt(t -> t.subject(email))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(member));
        mvc.perform(get(base + "/tasks/assignees").with(jwt().jwt(t -> t.subject(outsider)))).andExpect(status().isNotFound());
        for (String type : new String[] {"posts", "events", "albums", "tasks"}) {
            mvc.perform(get(base + "/" + type + "/page").with(jwt().jwt(t -> t.subject(outsider)))).andExpect(status().isNotFound());
            mvc.perform(get(base + "/" + type + "/page").param("page", "-1").with(jwt().jwt(t -> t.subject(email)))).andExpect(status().isBadRequest());
            mvc.perform(get(base + "/" + type + "/page").param("target", "0").with(jwt().jwt(t -> t.subject(email)))).andExpect(status().isNotFound());
        }
        mvc.perform(get(base + "/posts/page").param("page", "999999").with(jwt().jwt(t -> t.subject(email))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.page").value(2));
    }
}
