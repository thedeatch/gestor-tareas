package cl.gestortareas.api.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;

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
import cl.gestortareas.api.repository.CommunityMembershipRepository;
import cl.gestortareas.api.repository.CommunityRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CommunityPollControllerIntegrationTests {
    private static final String OWNER = "poll-owner@community.cl";
    private static final String MEMBER = "poll-member@community.cl";
    private static final String OUTSIDER = "poll-outsider@community.cl";
    @Autowired MockMvc mvc;
    @Autowired JdbcClient jdbc;
    @Autowired AppUserRepository users;
    @Autowired CommunityRepository communities;
    @Autowired CommunityMembershipRepository memberships;
    private Long groupId;
    private String url;

    @BeforeEach
    void setup() throws Exception {
        users.save(new AppUser("Organizadora", OWNER, "hash"));
        var member = users.save(new AppUser("Participante", MEMBER, "hash"));
        users.save(new AppUser("Visitante", OUTSIDER, "hash"));
        mvc.perform(post("/api/communities").with(jwt().jwt(token -> token.subject(OWNER)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Grupo de encuestas\",\"description\":\"Decisiones\",\"visibility\":\"PRIVATE\"}"))
            .andExpect(status().isCreated());
        groupId = jdbc.sql("SELECT MAX(id) FROM communities").query(Long.class).single();
        memberships.saveAndFlush(new CommunityMembership(
            communities.findById(groupId).orElseThrow(), member, CommunityRole.MEMBER));
        url = "/api/communities/" + groupId + "/polls";
    }

    @Test
    void membersCanCreateVoteAndChangeTheirSingleVoteWithIndependentResults() throws Exception {
        Long pollId = create(MEMBER);
        List<Long> options = options(pollId);
        String vote = url + "/" + pollId + "/vote";
        mvc.perform(put(vote).with(jwt().jwt(token -> token.subject(MEMBER)))
                .contentType(MediaType.APPLICATION_JSON).content("{\"optionId\":" + options.get(0) + "}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalVotes").value(1))
            .andExpect(jsonPath("$.selectedOptionId").value(options.get(0)))
            .andExpect(jsonPath("$.options[0].votes").value(1));
        mvc.perform(put(vote).with(jwt().jwt(token -> token.subject(MEMBER)))
                .contentType(MediaType.APPLICATION_JSON).content("{\"optionId\":" + options.get(0) + "}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalVotes").value(1));
        mvc.perform(put(vote).with(jwt().jwt(token -> token.subject(MEMBER)))
                .contentType(MediaType.APPLICATION_JSON).content("{\"optionId\":" + options.get(1) + "}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalVotes").value(1))
            .andExpect(jsonPath("$.options[0].votes").value(0))
            .andExpect(jsonPath("$.options[1].votes").value(1));
        mvc.perform(get(url).with(jwt().jwt(token -> token.subject(OWNER))))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].selectedOptionId").isEmpty())
            .andExpect(jsonPath("$[0].totalVotes").value(1));
        mvc.perform(put(vote).with(jwt().jwt(token -> token.subject(OWNER)))
                .contentType(MediaType.APPLICATION_JSON).content("{\"optionId\":" + options.get(0) + "}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalVotes").value(2));
        Long second = create(OWNER);
        mvc.perform(put(url + "/" + second + "/vote").with(jwt().jwt(token -> token.subject(MEMBER)))
                .contentType(MediaType.APPLICATION_JSON).content("{\"optionId\":" + options.get(0) + "}"))
            .andExpect(status().isBadRequest());
        mvc.perform(get(url).with(jwt().jwt(token -> token.subject(MEMBER))))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].totalVotes").value(0))
            .andExpect(jsonPath("$[1].selectedOptionId").value(options.get(1)))
            .andExpect(jsonPath("$[1].totalVotes").value(2));
        assertEquals(2L, jdbc.sql("SELECT COUNT(*) FROM community_poll_votes WHERE poll_id = :id")
            .param("id", pollId).query(Long.class).single());
    }

    @Test
    void protectsGroupAccessAndClosurePermissions() throws Exception {
        Long pollId = create(OWNER);
        String vote = url + "/" + pollId + "/vote";
        String close = url + "/" + pollId + "/close";
        mvc.perform(get(url).with(jwt().jwt(token -> token.subject(OUTSIDER)))).andExpect(status().isNotFound());
        mvc.perform(post(url).with(jwt().jwt(token -> token.subject(OUTSIDER)))
                .contentType(MediaType.APPLICATION_JSON).content(payload())).andExpect(status().isNotFound());
        mvc.perform(put(vote).with(jwt().jwt(token -> token.subject(OUTSIDER)))
                .contentType(MediaType.APPLICATION_JSON).content("{\"optionId\":" + options(pollId).get(0) + "}"))
            .andExpect(status().isNotFound());
        mvc.perform(put(close).with(jwt().jwt(token -> token.subject(OUTSIDER)))).andExpect(status().isNotFound());
        mvc.perform(put(close).with(jwt().jwt(token -> token.subject(MEMBER)))).andExpect(status().isForbidden());
        mvc.perform(put(url + "/0/vote").with(jwt().jwt(token -> token.subject(MEMBER)))
                .contentType(MediaType.APPLICATION_JSON).content("{\"optionId\":" + options(pollId).get(0) + "}"))
            .andExpect(status().isNotFound());
        mvc.perform(put(close).with(jwt().jwt(token -> token.subject(OWNER))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.closed").value(true));
        mvc.perform(put(close).with(jwt().jwt(token -> token.subject(OWNER)))).andExpect(status().isOk());
        mvc.perform(put(vote).with(jwt().jwt(token -> token.subject(MEMBER)))
                .contentType(MediaType.APPLICATION_JSON).content("{\"optionId\":" + options(pollId).get(0) + "}"))
            .andExpect(status().isConflict());
        Long memberPoll = create(MEMBER);
        mvc.perform(put(url + "/" + memberPoll + "/close").with(jwt().jwt(token -> token.subject(OWNER))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.closed").value(true));
    }

    @Test
    void validatesChoicesAndStopsVotingAtTheDeadline() throws Exception {
        for (String invalid : List.of(
            "{\"question\":\" \",\"options\":[\"Sí\",\"No\"]}",
            "{\"question\":\"Día\",\"options\":[\"Uno\"]}",
            "{\"question\":\"Día\",\"options\":[\" Viernes \",\"VIERNES\"]}",
            "{\"question\":\"Día\",\"options\":[\"Uno\",null]}",
            "{\"question\":\"Día\",\"options\":[\"Uno\",\"Dos\"],\"closesAt\":\"2020-01-01T00:00:00Z\"}"
        )) {
            mvc.perform(post(url).with(jwt().jwt(token -> token.subject(MEMBER)))
                    .contentType(MediaType.APPLICATION_JSON).content(invalid)).andExpect(status().isBadRequest());
        }
        assertEquals(0L, jdbc.sql("SELECT COUNT(*) FROM community_polls WHERE community_id = :id")
            .param("id", groupId).query(Long.class).single());
        Long pollId = create(MEMBER);
        jdbc.sql("UPDATE community_polls SET closes_at = :past WHERE id = :id")
            .param("past", OffsetDateTime.now().minusMinutes(1)).param("id", pollId).update();
        mvc.perform(get(url).with(jwt().jwt(token -> token.subject(MEMBER))))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].closed").value(true));
        mvc.perform(put(url + "/" + pollId + "/vote").with(jwt().jwt(token -> token.subject(MEMBER)))
                .contentType(MediaType.APPLICATION_JSON).content("{\"optionId\":" + options(pollId).get(0) + "}"))
            .andExpect(status().isConflict());
    }

    private Long create(String email) throws Exception {
        mvc.perform(post(url).with(jwt().jwt(token -> token.subject(email)))
                .contentType(MediaType.APPLICATION_JSON).content(payload()))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.options.length()").value(2))
            .andExpect(jsonPath("$.totalVotes").value(0)).andExpect(jsonPath("$.closed").value(false));
        return jdbc.sql("SELECT MAX(id) FROM community_polls WHERE community_id = :id")
            .param("id", groupId).query(Long.class).single();
    }

    private List<Long> options(Long pollId) {
        return jdbc.sql("SELECT id FROM community_poll_options WHERE poll_id = :id ORDER BY position")
            .param("id", pollId).query(Long.class).list();
    }

    private String payload() {
        return "{\"question\":\"¿Cuándo nos reunimos?\",\"options\":[\"Viernes\",\"Sábado\"]}";
    }
}
