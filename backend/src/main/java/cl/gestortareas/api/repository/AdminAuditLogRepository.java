package cl.gestortareas.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import cl.gestortareas.api.model.AdminAuditLog;

public interface AdminAuditLogRepository
    extends JpaRepository<AdminAuditLog, Long>,
        JpaSpecificationExecutor<AdminAuditLog> {
}
