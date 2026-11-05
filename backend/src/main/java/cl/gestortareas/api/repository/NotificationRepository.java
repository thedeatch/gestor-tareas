package cl.gestortareas.api.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import cl.gestortareas.api.model.AppUser;
import cl.gestortareas.api.model.Notification;

public interface NotificationRepository
    extends JpaRepository<Notification, Long> {

    Page<Notification> findAllByRecipient(
        AppUser recipient,
        Pageable pageable
    );

    Page<Notification> findAllByRecipientAndReadAtIsNull(
        AppUser recipient,
        Pageable pageable
    );

    long countByRecipientAndReadAtIsNull(AppUser recipient);

    Optional<Notification> findByIdAndRecipient(
        Long id,
        AppUser recipient
    );

    @Modifying
    @Query(value = """
        update notifications
        set read_at = CURRENT_TIMESTAMP
        where recipient_user_id = :recipientId
          and read_at is null
        """, nativeQuery = true)
    int markAllAsReadByRecipientId(@Param("recipientId") Long recipientId);
}
