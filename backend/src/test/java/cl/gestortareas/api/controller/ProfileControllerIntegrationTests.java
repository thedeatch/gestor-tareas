package cl.gestortareas.api.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import cl.gestortareas.api.model.AppUser;
import cl.gestortareas.api.repository.AppUserRepository;
import cl.gestortareas.api.service.JwtService;

@SpringBootTest
@AutoConfigureMockMvc
class ProfileControllerIntegrationTests {

    private final MockMvc mockMvc;
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private String token;

    @Autowired
    ProfileControllerIntegrationTests(
        MockMvc mockMvc,
        AppUserRepository userRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService
    ) {
        this.mockMvc = mockMvc;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        AppUser user = userRepository.save(new AppUser(
            "Usuario inicial",
            "usuario@ejemplo.cl",
            passwordEncoder.encode("secreto123")
        ));
        token = jwtService.generateToken(user);
    }

    @Test
    void shouldReturnAuthenticatedProfile() throws Exception {
        mockMvc.perform(get("/api/users/me")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Usuario inicial"))
            .andExpect(jsonPath("$.email").value("usuario@ejemplo.cl"))
            .andExpect(jsonPath("$.hasAvatar").value(false))
            .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void shouldUpdateNameAndEmailWithCurrentPassword() throws Exception {
        mockMvc.perform(patch("/api/users/me")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "name": "Nombre actualizado",
                        "email": "nuevo@ejemplo.cl",
                        "currentPassword": "secreto123"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Nombre actualizado"))
            .andExpect(jsonPath("$.email").value("nuevo@ejemplo.cl"))
            .andExpect(jsonPath("$.role").value("USER"))
            .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void shouldRejectProfileUpdateWithWrongPassword() throws Exception {
        mockMvc.perform(patch("/api/users/me")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "name": "Nombre actualizado",
                        "email": "nuevo@ejemplo.cl",
                        "currentPassword": "incorrecta"
                    }
                    """))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldChangePassword() throws Exception {
        mockMvc.perform(post("/api/users/me/password")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "currentPassword": "secreto123",
                        "newPassword": "nuevaClave123"
                    }
                    """))
            .andExpect(status().isNoContent());

        AppUser user = userRepository
            .findByEmailIgnoreCase("usuario@ejemplo.cl")
            .orElseThrow();
        assertTrue(passwordEncoder.matches(
            "nuevaClave123",
            user.getPasswordHash()
        ));
    }

    @Test
    void shouldUploadReadAndDeleteAvatar() throws Exception {
        byte[] image = new byte[] { 1, 2, 3, 4 };
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "avatar.png",
            MediaType.IMAGE_PNG_VALUE,
            image
        );

        mockMvc.perform(multipart("/api/users/me/avatar")
                .file(file)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/users/me/avatar")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.IMAGE_PNG))
            .andExpect(content().bytes(image));

        mockMvc.perform(delete("/api/users/me/avatar")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/users/me/avatar")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNotFound());
    }
}
