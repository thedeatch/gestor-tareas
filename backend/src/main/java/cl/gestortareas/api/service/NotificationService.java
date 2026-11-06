package cl.gestortareas.api.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import cl.gestortareas.api.dto.NotificationResponse;
import cl.gestortareas.api.model.AppUser;
import cl.gestortareas.api.model.Notification;
import cl.gestortareas.api.model.NotificationType;
import cl.gestortareas.api.repository.AppUserRepository;
import cl.gestortareas.api.repository.NotificationRepository;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final AppUserRepository userRepository;
    private final UserBlockService blocks;

    public NotificationService(
        NotificationRepository notificationRepository,
        AppUserRepository userRepository,
        UserBlockService blocks
    ) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.blocks = blocks;
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> findForUser(
        String email,
        boolean unreadOnly,
        int page,
        int size
    ) {
        AppUser user = requireUser(email);
        PageRequest pageable = PageRequest.of(
            Math.max(page, 0),
            Math.min(Math.max(size, 1), 100),
            Sort.by(Sort.Direction.DESC, "createdAt")
        );
        Page<Notification> notifications = unreadOnly
            ? notificationRepository.findAllByRecipientAndReadAtIsNull(
                user, pageable
            )
            : notificationRepository.findAllByRecipient(user, pageable);
        return notifications.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public long unreadCount(String email) {
        return notificationRepository.countByRecipientAndReadAtIsNull(
            requireUser(email)
        );
    }

    @Transactional
    public NotificationResponse markAsRead(String email, Long id) {
        AppUser user = requireUser(email);
        Notification notification = notificationRepository
            .findByIdAndRecipient(id, user)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Notificación no encontrada"
            ));
        notification.markAsRead();
        return toResponse(notification);
    }

    @Transactional
    public void markAllAsRead(String email) {
        AppUser user = requireUser(email);
        notificationRepository.markAllAsReadByRecipientId(user.getId());
    }

    @Transactional
    public void create(
        AppUser recipient,
        AppUser actor,
        NotificationType type,
        String title,
        String message,
        String targetUrl
    ) {
        if (actor != null && (type == NotificationType.COMMENT || type == NotificationType.REACTION)
            && blocks.between(recipient.getId(), actor.getId())) {
            return;
        }
        notificationRepository.save(new Notification(
            recipient, actor, type, title, message, targetUrl
        ));
    }

    public void createWelcome(AppUser user) {
        create(
            user,
            null,
            NotificationType.SYSTEM,
            "¡Te damos la bienvenida!",
            "Tu cuenta está lista. Puedes comenzar configurando tu perfil.",
            "/perfil"
        );
    }

    private AppUser requireUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Sesión no válida"
            ));
    }

    private NotificationResponse toResponse(Notification notification) {
        AppUser actor = notification.getActor();
        return new NotificationResponse(
            notification.getId(),
            notification.getType(),
            notification.getTitle(),
            notification.getMessage(),
            notification.getTargetUrl(),
            actor == null ? null : actor.getName(),
            actor == null ? null : actor.getEmail(),
            notification.getReadAt(),
            notification.getCreatedAt()
        );
    }
}
