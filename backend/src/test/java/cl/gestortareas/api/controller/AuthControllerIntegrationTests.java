package cl.gestortareas.api.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import cl.gestortareas.api.model.UserRole;
import cl.gestortareas.api.repository.AppUserRepository;
import cl.gestortareas.api.repository.AdminAuditLogRepository;
import cl.gestortareas.api.service.JwtService;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTests {

    private final MockMvc mockMvc;
    private final AppUserRepository userRepository;
    private final AdminAuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Autowired
    AuthControllerIntegrationTests(
        MockMvc mockMvc,
        AppUserRepository userRepository,
        AdminAuditLogRepository auditLogRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService
    ) {
        this.mockMvc = mockMvc;
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @BeforeEach
    void cleanDatabase() {
        auditLogRepository.deleteAll();
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
            .andExpect(jsonPath("$.role").value("USER"))
            .andExpect(jsonPath("$.token").isNotEmpty());

        AppUser user = userRepository.findByEmailIgnoreCase("usuario@ejemplo.cl")
            .orElseThrow();

        assertTrue(user.getName().equals("Usuario de prueba"));
        assertFalse(user.getPasswordHash().equals("secreto123"));
        assertTrue(passwordEncoder.matches("secreto123", user.getPasswordHash()));
        assertTrue(user.getRole() == UserRole.USER);
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
            .andExpect(jsonPath("$.role").value("USER"))
            .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void shouldRejectAdminEndpointForRegularUser() throws Exception {
        AppUser user = userRepository.save(new AppUser(
            "Usuario normal",
            "usuario@ejemplo.cl",
            passwordEncoder.encode("secreto123")
        ));

        mockMvc.perform(get("/api/admin/access")
                .header("Authorization", "Bearer " + jwtService.generateToken(user)))
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowAdminEndpointForAdminUser() throws Exception {
        AppUser admin = new AppUser(
            "Administrador",
            "admin@ejemplo.cl",
            passwordEncoder.encode("secreto123")
        );
        admin.changeRole(UserRole.ADMIN);
        userRepository.save(admin);

        mockMvc.perform(get("/api/admin/access")
                .header("Authorization", "Bearer " + jwtService.generateToken(admin)))
            .andExpect(status().isNoContent());
    }

    @Test
    void shouldAllowAdminToListUsersAndChangeTheirRole() throws Exception {
        AppUser admin = new AppUser(
            "Administrador",
            "admin@ejemplo.cl",
            passwordEncoder.encode("secreto123")
        );
        admin.changeRole(UserRole.ADMIN);
        userRepository.save(admin);

        AppUser user = userRepository.save(new AppUser(
            "Usuario normal",
            "usuario@ejemplo.cl",
            passwordEncoder.encode("secreto123")
        ));
        String adminToken = jwtService.generateToken(admin);
        String userToken = jwtService.generateToken(user);

        mockMvc.perform(get("/api/admin/users")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.content[0].email").exists());

        mockMvc.perform(patch("/api/admin/users/{id}/role", user.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "role": "MODERATOR"
                    }
                    """))
            .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/api/admin/users/{id}/role", user.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "role": "MODERATOR",
                        "reason": "Asignación de responsabilidades de moderación"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("MODERATOR"));

        AppUser updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertTrue(updatedUser.getRole() == UserRole.MODERATOR);

        mockMvc.perform(get("/api/tasks")
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldApplyModeratorPermissions() throws Exception {
        AppUser admin = new AppUser(
            "Administrador",
            "admin@ejemplo.cl",
            passwordEncoder.encode("secreto123")
        );
        admin.changeRole(UserRole.ADMIN);
        userRepository.save(admin);

        AppUser moderator = new AppUser(
            "Moderador",
            "moderador@ejemplo.cl",
            passwordEncoder.encode("secreto123")
        );
        moderator.changeRole(UserRole.MODERATOR);
        userRepository.save(moderator);

        AppUser user = userRepository.save(new AppUser(
            "Usuario normal",
            "usuario@ejemplo.cl",
            passwordEncoder.encode("secreto123")
        ));
        String moderatorToken = jwtService.generateToken(moderator);

        mockMvc.perform(get("/api/admin/users")
                .header("Authorization", "Bearer " + moderatorToken))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/audit")
                .header("Authorization", "Bearer " + moderatorToken))
            .andExpect(status().isOk());

        mockMvc.perform(patch("/api/admin/users/{id}/role", user.getId())
                .header("Authorization", "Bearer " + moderatorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "role": "MODERATOR"
                    }
                    """))
            .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/admin/users/{id}/status", admin.getId())
                .header("Authorization", "Bearer " + moderatorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "active": false,
                        "reason": "Intento no autorizado"
                    }
                    """))
            .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/admin/users/{id}/status", user.getId())
                .header("Authorization", "Bearer " + moderatorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "active": false,
                        "reason": "Incumplimiento de las normas"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(patch("/api/admin/users/{id}/status", user.getId())
                .header("Authorization", "Bearer " + moderatorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "active": true
                    }
                    """))
            .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/api/admin/users/{id}/status", user.getId())
                .header("Authorization", "Bearer " + moderatorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "active": true,
                        "reason": "Revisión completada satisfactoriamente"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void shouldPreventAdminFromRemovingOwnRole() throws Exception {
        AppUser admin = new AppUser(
            "Administrador",
            "admin@ejemplo.cl",
            passwordEncoder.encode("secreto123")
        );
        admin.changeRole(UserRole.ADMIN);
        userRepository.save(admin);

        mockMvc.perform(patch("/api/admin/users/{id}/role", admin.getId())
                .header(
                    "Authorization",
                    "Bearer " + jwtService.generateToken(admin)
                )
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "role": "USER",
                        "reason": "Prueba de autoprotección"
                    }
                    """))
            .andExpect(status().isConflict());
    }

    @Test
    void shouldSuspendUserAndRejectExistingToken() throws Exception {
        AppUser admin = new AppUser(
            "Administrador",
            "admin@ejemplo.cl",
            passwordEncoder.encode("secreto123")
        );
        admin.changeRole(UserRole.ADMIN);
        userRepository.save(admin);

        AppUser user = userRepository.save(new AppUser(
            "Usuario suspendible",
            "usuario@ejemplo.cl",
            passwordEncoder.encode("secreto123")
        ));
        String userToken = jwtService.generateToken(user);

        mockMvc.perform(patch("/api/admin/users/{id}/status", user.getId())
                .header(
                    "Authorization",
                    "Bearer " + jwtService.generateToken(admin)
                )
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "active": false
                    }
                    """))
            .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/api/admin/users/{id}/status", user.getId())
                .header(
                    "Authorization",
                    "Bearer " + jwtService.generateToken(admin)
                )
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "active": false,
                        "reason": "Incumplimiento reiterado de las normas"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/api/tasks")
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/audit")
                .header(
                    "Authorization",
                    "Bearer " + jwtService.generateToken(admin)
                ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].actorEmail")
                .value("admin@ejemplo.cl"))
            .andExpect(jsonPath("$.content[0].targetEmail")
                .value("usuario@ejemplo.cl"))
            .andExpect(jsonPath("$.content[0].action")
                .value("ACCOUNT_SUSPENDED"))
            .andExpect(jsonPath("$.content[0].previousValue")
                .value("ACTIVE"))
            .andExpect(jsonPath("$.content[0].newValue")
                .value("SUSPENDED"))
            .andExpect(jsonPath("$.content[0].reason")
                .value("Incumplimiento reiterado de las normas"));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "email": "usuario@ejemplo.cl",
                        "password": "secreto123"
                    }
                    """))
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldPreventAdminFromSuspendingOwnAccount() throws Exception {
        AppUser admin = new AppUser(
            "Administrador",
            "admin@ejemplo.cl",
            passwordEncoder.encode("secreto123")
        );
        admin.changeRole(UserRole.ADMIN);
        userRepository.save(admin);

        mockMvc.perform(patch("/api/admin/users/{id}/status", admin.getId())
                .header(
                    "Authorization",
                    "Bearer " + jwtService.generateToken(admin)
                )
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "active": false,
                        "reason": "Prueba de autosuspensión"
                    }
                    """))
            .andExpect(status().isConflict());
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
