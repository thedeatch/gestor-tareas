package cl.gestortareas.api.dto;

import java.util.List;

import org.springframework.data.domain.Page;

public record AdminAuditPageResponse(
    List<AdminAuditResponse> content,
    int number,
    int size,
    long totalElements,
    int totalPages,
    boolean first,
    boolean last
) {
    public static AdminAuditPageResponse from(Page<AdminAuditResponse> page) {
        return new AdminAuditPageResponse(
            page.getContent(), page.getNumber(), page.getSize(),
            page.getTotalElements(), page.getTotalPages(),
            page.isFirst(), page.isLast()
        );
    }
}
