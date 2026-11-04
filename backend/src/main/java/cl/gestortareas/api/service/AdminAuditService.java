package cl.gestortareas.api.service;

import java.time.LocalDate;
import java.time.ZoneOffset;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import cl.gestortareas.api.dto.AdminAuditResponse;
import cl.gestortareas.api.model.AdminAuditAction;
import cl.gestortareas.api.model.AdminAuditLog;
import cl.gestortareas.api.model.AppUser;
import cl.gestortareas.api.repository.AdminAuditLogRepository;

@Service
public class AdminAuditService {

    private final AdminAuditLogRepository auditRepository;

    public AdminAuditService(AdminAuditLogRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    public void record(
        String actorEmail,
        AppUser target,
        AdminAuditAction action,
        String previousValue,
        String newValue
    ) {
        record(actorEmail, target, action, previousValue, newValue, null);
    }

    public void record(
        String actorEmail,
        AppUser target,
        AdminAuditAction action,
        String previousValue,
        String newValue,
        String reason
    ) {
        auditRepository.save(new AdminAuditLog(
            actorEmail, target, action, previousValue, newValue, reason
        ));
    }

    public Page<AdminAuditResponse> findAll(
        String search,
        AdminAuditAction action,
        LocalDate fromDate,
        LocalDate toDate,
        int page,
        int size
    ) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La fecha inicial no puede ser posterior a la fecha final"
            );
        }

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        String normalizedSearch = search == null ? "" : search.trim();
        Specification<AdminAuditLog> filters = (root, query, builder) ->
            builder.conjunction();

        if (!normalizedSearch.isEmpty()) {
            String pattern = "%" + normalizedSearch.toLowerCase() + "%";
            filters = filters.and((root, query, builder) -> builder.or(
                builder.like(builder.lower(root.get("actorEmail")), pattern),
                builder.like(builder.lower(root.get("targetEmail")), pattern)
            ));
        }
        if (action != null) {
            filters = filters.and((root, query, builder) ->
                builder.equal(root.get("action"), action)
            );
        }
        if (fromDate != null) {
            var fromInstant = fromDate.atStartOfDay(ZoneOffset.UTC).toInstant();
            filters = filters.and((root, query, builder) ->
                builder.greaterThanOrEqualTo(root.get("createdAt"), fromInstant)
            );
        }
        if (toDate != null) {
            var toInstant = toDate.plusDays(1)
                .atStartOfDay(ZoneOffset.UTC).toInstant();
            filters = filters.and((root, query, builder) ->
                builder.lessThan(root.get("createdAt"), toInstant)
            );
        }

        return auditRepository.findAll(
            filters,
            PageRequest.of(
                safePage,
                safeSize,
                Sort.by(Sort.Direction.DESC, "createdAt")
            )
        ).map(this::toResponse);
    }

    private AdminAuditResponse toResponse(AdminAuditLog log) {
        return new AdminAuditResponse(
            log.getId(), log.getActorEmail(), log.getTargetUserId(),
            log.getTargetEmail(), log.getAction(), log.getPreviousValue(),
            log.getNewValue(), log.getReason(), log.getCreatedAt()
        );
    }
}
