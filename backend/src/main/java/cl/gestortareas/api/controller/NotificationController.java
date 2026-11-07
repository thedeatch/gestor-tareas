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

import org.springframework.web.context.request.async.DeferredResult;

import cl.gestortareas.api.dto.NotificationPageResponse;
import cl.gestortareas.api.dto.NotificationResponse;
import cl.gestortareas.api.dto.UnreadNotificationCountResponse;
import cl.gestortareas.api.service.NotificationService;
import cl.gestortareas.api.service.NotificationService.LiveState;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    private final LongPoll polling;

    public NotificationController(NotificationService notificationService, LongPoll polling) {
        this.notificationService = notificationService;
        this.polling = polling;
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

    @GetMapping("/live")
    public DeferredResult<LiveState> live(@AuthenticationPrincipal Jwt jwt,
        @RequestParam(required = false) Long latestId, @RequestParam(required = false) Long count) {
        String email = jwt.getSubject();
        return polling.await(() -> notificationService.liveState(email),
            state -> latestId == null || count == null || state.latestId() != latestId || state.count() != count,
            null);
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
