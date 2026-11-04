package cl.gestortareas.api.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

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

    public Page<AdminAuditResponse> findAll(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        return auditRepository.findAll(PageRequest.of(
            safePage,
            safeSize,
            Sort.by(Sort.Direction.DESC, "createdAt")
        )).map(this::toResponse);
    }

    private AdminAuditResponse toResponse(AdminAuditLog log) {
        return new AdminAuditResponse(
            log.getId(), log.getActorEmail(), log.getTargetUserId(),
            log.getTargetEmail(), log.getAction(), log.getPreviousValue(),
            log.getNewValue(), log.getReason(), log.getCreatedAt()
        );
    }
}
