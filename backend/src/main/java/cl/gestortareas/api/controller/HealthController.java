package cl.gestortareas.api.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/health")
@Tag(name = "Salud", description = "Comprobación del estado de la API")
public class HealthController {

    @GetMapping
    @Operation(summary = "Comprobar que la API está disponible")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
