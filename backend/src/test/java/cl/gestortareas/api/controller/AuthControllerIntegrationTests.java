package cl.gestortareas.api.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import cl.gestortareas.api.model.AppUser;
import cl.gestortareas.api.repository.AppUserRepository;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTests {

    private final MockMvc mockMvc;
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    AuthControllerIntegrationTests(
        MockMvc mockMvc,
        AppUserRepository userRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.mockMvc = mockMvc;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    @Test
    void shouldRegisterUserAndHashPassword() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "name": "Usuario de prueba",
                        "email": "Usuario@Ejemplo.cl",
                        "password": "secreto123"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Usuario de prueba"))
            .andExpect(jsonPath("$.email").value("usuario@ejemplo.cl"))
            .andExpect(jsonPath("$.token").isNotEmpty());

        AppUser user = userRepository.findByEmailIgnoreCase("usuario@ejemplo.cl")
            .orElseThrow();

        assertTrue(user.getName().equals("Usuario de prueba"));
        assertFalse(user.getPasswordHash().equals("secreto123"));
        assertTrue(passwordEncoder.matches("secreto123", user.getPasswordHash()));
    }

    @Test
    void shouldLoginWithValidCredentials() throws Exception {
        userRepository.save(new AppUser(
            "Usuario de prueba",
            "usuario@ejemplo.cl",
            passwordEncoder.encode("secreto123")
        ));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "email": "usuario@ejemplo.cl",
                        "password": "secreto123"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Usuario de prueba"))
            .andExpect(jsonPath("$.email").value("usuario@ejemplo.cl"))
            .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void shouldRejectAnAlreadyRegisteredEmail() throws Exception {
        userRepository.save(new AppUser(
            "Usuario existente",
            "usuario@ejemplo.cl",
            passwordEncoder.encode("secreto123")
        ));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "name": "Otro nombre",
                        "email": "usuario@ejemplo.cl",
                        "password": "otraClave123"
                    }
                    """))
            .andExpect(status().isConflict());
    }

    @Test
    void shouldRejectTaskRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/api/tasks"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowCorsRequestsFromAngular() throws Exception {
        mockMvc.perform(options("/api/auth/register")
                .header("Origin", "http://localhost:4200")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type"))
            .andExpect(status().isOk())
            .andExpect(header().string(
                "Access-Control-Allow-Origin",
                "http://localhost:4200"
            ));
    }
}
