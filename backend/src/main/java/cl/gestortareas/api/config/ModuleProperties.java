package cl.gestortareas.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.modules")
public record ModuleProperties(
    boolean tasksEnabled,
    boolean communitiesEnabled,
    boolean usersEnabled,
    boolean auditEnabled
) {
}
