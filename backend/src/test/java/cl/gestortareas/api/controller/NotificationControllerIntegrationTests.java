package cl.gestortareas.api.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import cl.gestortareas.api.model.AppUser;
import cl.gestortareas.api.model.NotificationType;
import cl.gestortareas.api.repository.AppUserRepository;
import cl.gestortareas.api.repository.NotificationRepository;
import cl.gestortareas.api.service.NotificationService;

@SpringBootTest
@AutoConfigureMockMvc
class NotificationControllerIntegrationTests {

    private static final String USER_EMAIL = "notificaciones@gestor.cl";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationService notificationService;

    @MockitoBean
    private JavaMailSender mailSender;

    private AppUser user;

    @BeforeEach
    void cleanDatabase() {
        notificationRepository.deleteAll();
        userRepository.deleteAll();
        user = userRepository.save(new AppUser(
            "Usuario de notificaciones",
            USER_EMAIL,
            "hash-de-prueba"
        ));
    }

    @Test
    void shouldListCountAndReadOwnedNotifications() throws Exception {
        notificationService.createWelcome(user);
        Long notificationId = notificationRepository.findAll().get(0).getId();

        mockMvc.perform(get("/api/notifications")
                .with(jwt().jwt(token -> token.subject(USER_EMAIL))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].type").value("SYSTEM"))
            .andExpect(jsonPath("$.content[0].readAt").isEmpty());

        mockMvc.perform(get("/api/notifications/unread-count")
                .with(jwt().jwt(token -> token.subject(USER_EMAIL))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count").value(1));

        mockMvc.perform(patch(
                "/api/notifications/{id}/read",
                notificationId
            ).with(jwt().jwt(token -> token.subject(USER_EMAIL))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.readAt").isNotEmpty());

        mockMvc.perform(get("/api/notifications/unread-count")
                .with(jwt().jwt(token -> token.subject(USER_EMAIL))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count").value(0));
    }

    @Test
    void shouldNotExposeAnotherUsersNotification() throws Exception {
        AppUser other = userRepository.save(new AppUser(
            "Otro usuario",
            "otro-notificaciones@gestor.cl",
            "hash-de-prueba"
        ));
        notificationService.createWelcome(other);
        Long notificationId = notificationRepository.findAll().get(0).getId();

        mockMvc.perform(patch(
                "/api/notifications/{id}/read",
                notificationId
            ).with(jwt().jwt(token -> token.subject(USER_EMAIL))))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldMarkAllNotificationsAsRead() throws Exception {
        notificationService.createWelcome(user);
        notificationService.createWelcome(user);

        mockMvc.perform(patch("/api/notifications/read-all")
                .with(jwt().jwt(token -> token.subject(USER_EMAIL))))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/notifications/unread-count")
                .with(jwt().jwt(token -> token.subject(USER_EMAIL))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count").value(0));
    }

    @Test
    void shouldApplyPreferencesOnlyToNewNotificationsForTheRecipient() throws Exception {
        String prefs = "/api/users/me/notification-preferences";
        var viewer = jwt().jwt(token -> token.subject(USER_EMAIL));
        var other = userRepository.save(new AppUser("Otro usuario", "preferencias@gestor.cl", "hash"));
        var otherJwt = jwt().jwt(token -> token.subject(other.getEmail()));

        mockMvc.perform(get(prefs).with(viewer)).andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
        notificationService.create(user, other, NotificationType.COMMENT, "Comentario anterior", "Texto", null);

        var off = "{\"enabled\":false}";
        mockMvc.perform(put(prefs + "/COMMENT").with(viewer).contentType(MediaType.APPLICATION_JSON).content(off))
            .andExpect(status().isNoContent());
        mockMvc.perform(put(prefs + "/COMMENT").with(viewer).contentType(MediaType.APPLICATION_JSON).content(off))
            .andExpect(status().isNoContent());
        mockMvc.perform(put(prefs + "/MODERATION").with(viewer).contentType(MediaType.APPLICATION_JSON).content(off))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get(prefs).with(viewer)).andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0]").value("COMMENT"));
        mockMvc.perform(get(prefs).with(otherJwt)).andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));

        notificationService.create(user, other, NotificationType.COMMENT, "Comentario silenciado", "Texto", null);
        notificationService.create(user, other, NotificationType.MODERATION, "Moderación", "Texto", null);
        notificationService.create(other, user, NotificationType.COMMENT, "Comentario para otro usuario", "Texto", null);
        mockMvc.perform(get("/api/notifications/unread-count").with(viewer)).andExpect(status().isOk())
            .andExpect(jsonPath("$.count").value(2));
        mockMvc.perform(get("/api/notifications/unread-count").with(otherJwt)).andExpect(status().isOk())
            .andExpect(jsonPath("$.count").value(1));

        mockMvc.perform(put(prefs + "/COMMENT").with(viewer).contentType(MediaType.APPLICATION_JSON)
                .content("{\"enabled\":true}"))
            .andExpect(status().isNoContent());
        notificationService.create(user, other, NotificationType.COMMENT, "Comentario nuevo", "Texto", null);
        mockMvc.perform(get("/api/notifications/unread-count").with(viewer)).andExpect(status().isOk())
            .andExpect(jsonPath("$.count").value(3));
    }

    @Test
    void shouldWakeLiveNotificationsOnlyForTheCurrentRecipient() throws Exception {
        var viewer = jwt().jwt(token -> token.subject(USER_EMAIL));
        var other = userRepository.save(new AppUser("Otra persona", "live@gestor.cl", "hash"));
        var initial = mockMvc.perform(get("/api/notifications/live").with(viewer))
            .andExpect(request().asyncStarted()).andReturn();
        mockMvc.perform(asyncDispatch(initial)).andExpect(status().isOk())
            .andExpect(jsonPath("$.count").value(0))
            .andExpect(jsonPath("$.latestId").value(0));

        var waiting = mockMvc.perform(get("/api/notifications/live")
                .param("latestId", "0").param("count", "0").with(viewer))
            .andExpect(request().asyncStarted()).andReturn();
        notificationService.createWelcome(other);
        notificationService.createWelcome(user);
        waiting.getAsyncResult(5000);
        Long id = notificationRepository.findAllByRecipient(user,
            org.springframework.data.domain.Pageable.unpaged()).getContent().get(0).getId();
        mockMvc.perform(asyncDispatch(waiting)).andExpect(status().isOk())
            .andExpect(jsonPath("$.count").value(1))
            .andExpect(jsonPath("$.latestId").value(id));

        var reading = mockMvc.perform(get("/api/notifications/live")
                .param("latestId", id.toString()).param("count", "1").with(viewer))
            .andExpect(request().asyncStarted()).andReturn();
        mockMvc.perform(patch("/api/notifications/" + id + "/read").with(viewer)).andExpect(status().isOk());
        reading.getAsyncResult(5000);
        mockMvc.perform(asyncDispatch(reading)).andExpect(status().isOk())
            .andExpect(jsonPath("$.count").value(0))
            .andExpect(jsonPath("$.latestId").value(id));
    }
}
