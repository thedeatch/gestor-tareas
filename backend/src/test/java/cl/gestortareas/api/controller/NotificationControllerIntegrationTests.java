package cl.gestortareas.api.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import cl.gestortareas.api.model.AppUser;
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
}
