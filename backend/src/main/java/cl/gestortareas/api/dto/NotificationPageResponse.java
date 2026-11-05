package cl.gestortareas.api.dto;

import java.util.List;

import org.springframework.data.domain.Page;

public record NotificationPageResponse(
    List<NotificationResponse> content,
    int number,
    int size,
    long totalElements,
    int totalPages,
    boolean first,
    boolean last
) {
    public static NotificationPageResponse from(
        Page<NotificationResponse> page
    ) {
        return new NotificationPageResponse(
            page.getContent(), page.getNumber(), page.getSize(),
            page.getTotalElements(), page.getTotalPages(),
            page.isFirst(), page.isLast()
        );
    }
}
