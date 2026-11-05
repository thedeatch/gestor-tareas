package cl.gestortareas.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cl.gestortareas.api.dto.NotificationPageResponse;
import cl.gestortareas.api.dto.NotificationResponse;
import cl.gestortareas.api.dto.UnreadNotificationCountResponse;
import cl.gestortareas.api.service.NotificationService;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public NotificationPageResponse findAll(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(defaultValue = "false") boolean unreadOnly,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return NotificationPageResponse.from(
            notificationService.findForUser(
                jwt.getSubject(), unreadOnly, page, size
            )
        );
    }

    @GetMapping("/unread-count")
    public UnreadNotificationCountResponse unreadCount(
        @AuthenticationPrincipal Jwt jwt
    ) {
        return new UnreadNotificationCountResponse(
            notificationService.unreadCount(jwt.getSubject())
        );
    }

    @PatchMapping("/{id}/read")
    public NotificationResponse markAsRead(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable Long id
    ) {
        return notificationService.markAsRead(jwt.getSubject(), id);
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(
        @AuthenticationPrincipal Jwt jwt
    ) {
        notificationService.markAllAsRead(jwt.getSubject());
        return ResponseEntity.noContent().build();
    }
}
