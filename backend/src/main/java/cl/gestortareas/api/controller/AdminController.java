package cl.gestortareas.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cl.gestortareas.api.dto.AdminUserResponse;
import cl.gestortareas.api.dto.AdminUserPageResponse;
import cl.gestortareas.api.dto.UpdateUserRoleRequest;
import cl.gestortareas.api.service.AdminUserService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminUserService adminUserService;

    public AdminController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping("/access")
    public ResponseEntity<Void> verifyAccess() {
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users")
    public AdminUserPageResponse findUsers(
        @RequestParam(defaultValue = "") String search,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return AdminUserPageResponse.from(
            adminUserService.findUsers(search, page, size)
        );
    }

    @PatchMapping("/users/{id}/role")
    public AdminUserResponse updateRole(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable Long id,
        @Valid @RequestBody UpdateUserRoleRequest request
    ) {
        return adminUserService.updateRole(
            jwt.getSubject(),
            id,
            request.role()
        );
    }
}
