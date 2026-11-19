package cl.gestortareas.api.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class CommunitySearchControllerIntegrationTests {
    private static final String OWNER = "search-owner@community.cl";
    private static final String MEMBER = "search-member@community.cl";
    private static final String OUTSIDER = "search-outsider@community.cl";
    @Autowired MockMvc mvc;
    @Autowired JdbcClient jdbc;
    @Autowired AppUserRepository users;
    @Autowired CommunityRepository communities;
    @Autowired CommunityMembershipRepository memberships;
    private Long groupId;
    private Long ownerId;
    private String url;

    @BeforeEach
    void setup() throws Exception {
        ownerId = users.save(new AppUser("Organizadora", OWNER, "hash")).getId();
        var member = users.save(new AppUser("Miembro", MEMBER, "hash"));
        users.save(new AppUser("Visitante", OUTSIDER, "hash"));
        groupId = createGroup("Búsqueda de grupo");
        memberships.saveAndFlush(new CommunityMembership(communities.findById(groupId).orElseThrow(),
            member, CommunityRole.MEMBER));
        url = "/api/communities/" + groupId + "/search";
        addPost(groupId, "Anuncio", "Reunión de la comunidad Angular");
        jdbc.sql("INSERT INTO community_tasks (community_id, description) VALUES (:groupId, :description)")
            .param("groupId", groupId).param("description", "Preparar reunión Angular").update();
        jdbc.sql("""
            INSERT INTO community_events (community_id, title, description, starts_at)
            VALUES (:groupId, 'Reunión Angular', 'Encuentro del grupo', CURRENT_TIMESTAMP)
            """).param("groupId", groupId).update();
        jdbc.sql("INSERT INTO community_albums (community_id, title, description) VALUES (:groupId, :title, :description)")
            .param("groupId", groupId).param("title", "Fotos de la reunión").param("description", "Encuentro Angular").update();
    }

    @Test
    void searchesAllFourTypesWithCaseAndAccentInsensitiveWords() throws Exception {
        mvc.perform(get(url).param("q", "REUNION angular").with(jwt().jwt(token -> token.subject(MEMBER))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(4))
            .andExpect(jsonPath("$.items[0].kind").value("EVENT"))
            .andExpect(jsonPath("$.items[0].title").value("Reunión Angular"));
        for (String kind : new String[] {"POST", "TASK", "EVENT", "ALBUM"}) {
            mvc.perform(get(url).param("q", "angular REUNIÓN").param("kind", kind)
                    .with(jwt().jwt(token -> token.subject(MEMBER))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].kind").value(kind));
        }
        addPost(groupId, "Texto pegado", "Reunio\u0301n Angular");
        mvc.perform(get(url).param("q", "reunion angular").param("kind", "POST")
                .with(jwt().jwt(token -> token.subject(MEMBER))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(2));
    }

    @Test
    void protectsMembershipAndExcludesOtherGroupsAndPersonalTasks() throws Exception {
        Long otherGroup = createGroup("Otro grupo");
        addPost(otherGroup, "Privado", "Reunión Angular secreta");
        mvc.perform(post("/api/tasks").with(jwt().jwt(token -> token.subject(OWNER)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"description\":\"Reunión Angular personal\",\"completed\":false}"))
            .andExpect(status().isCreated());
        mvc.perform(get(url).param("q", "reunion angular").with(jwt().jwt(token -> token.subject(OWNER))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(4));
        mvc.perform(get(url).param("q", "reunion").with(jwt().jwt(token -> token.subject(OUTSIDER))))
            .andExpect(status().isNotFound());
        mvc.perform(get(url).with(jwt().jwt(token -> token.subject(OUTSIDER)))).andExpect(status().isNotFound());
    }

    @Test
    void treatsWildcardsAndSqlCharactersAsLiteralSearchText() throws Exception {
        addPost(groupId, "Descuento", "100% real y archivo_2016");
        addPost(groupId, "Otro", "100XYZ real y archivoX2016");
        mvc.perform(get(url).param("q", "100%").with(jwt().jwt(token -> token.subject(MEMBER))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1));
        mvc.perform(get(url).param("q", "archivo_2016").with(jwt().jwt(token -> token.subject(MEMBER))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1));
        mvc.perform(get(url).param("q", "' OR 1=1 --").with(jwt().jwt(token -> token.subject(MEMBER))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void paginatesAndValidatesEmptyAndOversizedQueries() throws Exception {
        for (int index = 0; index < 21; index++) addPost(groupId, "Paginar " + index, "Resultados");
        mvc.perform(get(url).param("q", "paginar").with(jwt().jwt(token -> token.subject(MEMBER))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(21))
            .andExpect(jsonPath("$.items.length()").value(20));
        mvc.perform(get(url).param("q", "paginar").param("page", "1")
                .with(jwt().jwt(token -> token.subject(MEMBER))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1));
        mvc.perform(get(url).param("q", "  ").with(jwt().jwt(token -> token.subject(MEMBER))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
        mvc.perform(get(url).param("q", "a".repeat(201)).with(jwt().jwt(token -> token.subject(MEMBER))))
            .andExpect(status().isBadRequest());
        mvc.perform(get(url).param("page", "-1").with(jwt().jwt(token -> token.subject(MEMBER))))
            .andExpect(status().isBadRequest());
    }

    private Long createGroup(String name) throws Exception {
        mvc.perform(post("/api/communities").with(jwt().jwt(token -> token.subject(OWNER)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + name + "\",\"description\":\"Pruebas\",\"visibility\":\"PRIVATE\"}"))
            .andExpect(status().isCreated());
        return jdbc.sql("SELECT MAX(id) FROM communities").query(Long.class).single();
    }

    private void addPost(Long groupId, String title, String content) {
        jdbc.sql("""
            INSERT INTO community_posts (community_id, author_user_id, title, content, created_at, updated_at)
            VALUES (:groupId, :userId, :title, :content, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """).param("groupId", groupId).param("userId", ownerId).param("title", title).param("content", content).update();
    }
}
