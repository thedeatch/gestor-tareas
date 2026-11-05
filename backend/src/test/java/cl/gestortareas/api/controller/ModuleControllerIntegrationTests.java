package cl.gestortareas.api.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
    "app.modules.tasks-enabled=false",
    "app.modules.users-enabled=true",
    "app.modules.audit-enabled=false"
})
@AutoConfigureMockMvc
class ModuleControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JavaMailSender mailSender;

    @Test
    void shouldExposeConfiguredModuleAvailability() throws Exception {
        mockMvc.perform(get("/api/modules"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tasks").value(false))
            .andExpect(jsonPath("$.profile").value(true))
            .andExpect(jsonPath("$.users").value(true))
            .andExpect(jsonPath("$.audit").value(false));
    }
}
