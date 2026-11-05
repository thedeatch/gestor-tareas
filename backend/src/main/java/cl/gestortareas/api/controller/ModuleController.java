package cl.gestortareas.api.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.gestortareas.api.config.ModuleProperties;

@RestController
@RequestMapping("/api/modules")
public class ModuleController {

    private final ModuleProperties modules;

    public ModuleController(ModuleProperties modules) {
        this.modules = modules;
    }

    @GetMapping
    public Map<String, Boolean> findAll() {
        return Map.of(
            "tasks", modules.tasksEnabled(),
            "profile", true,
            "users", modules.usersEnabled(),
            "audit", modules.auditEnabled()
        );
    }
}
