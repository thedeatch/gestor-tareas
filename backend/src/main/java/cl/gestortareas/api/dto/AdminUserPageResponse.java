package cl.gestortareas.api.dto;

import java.util.List;

import org.springframework.data.domain.Page;

public record AdminUserPageResponse(
    List<AdminUserResponse> content,
    int number,
    int size,
    long totalElements,
    int totalPages,
    boolean first,
    boolean last
) {
    public static AdminUserPageResponse from(Page<AdminUserResponse> page) {
        return new AdminUserPageResponse(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.isFirst(),
            page.isLast()
        );
    }
}
