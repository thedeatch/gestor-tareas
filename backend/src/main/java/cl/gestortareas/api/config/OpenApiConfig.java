package cl.gestortareas.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI gestorTareasOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("API Gestor de Tareas")
                .description(
                    "API REST para crear, consultar, actualizar "
                    + "y eliminar tareas."
                )
                .version("1.0.0")
            );
    }
}