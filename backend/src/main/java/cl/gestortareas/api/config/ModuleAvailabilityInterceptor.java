package cl.gestortareas.api.config;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ModuleAvailabilityInterceptor implements HandlerInterceptor {

    private final ModuleProperties modules;

    public ModuleAvailabilityInterceptor(ModuleProperties modules) {
        this.modules = modules;
    }

    @Override
    public boolean preHandle(
        HttpServletRequest request,
        HttpServletResponse response,
        Object handler
    ) {
        String path = request.getRequestURI();

        if (path.startsWith("/api/tasks") && !modules.tasksEnabled()) {
            disabled();
        }
        if (path.startsWith("/api/admin/users") && !modules.usersEnabled()) {
            disabled();
        }
        if (path.startsWith("/api/admin/audit") && !modules.auditEnabled()) {
            disabled();
        }

        return true;
    }

    private void disabled() {
        throw new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Este módulo no está habilitado."
        );
    }
}
