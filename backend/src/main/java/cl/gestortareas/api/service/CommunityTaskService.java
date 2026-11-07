package cl.gestortareas.api.service;

import java.sql.Types;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import cl.gestortareas.api.model.AppUser;
import cl.gestortareas.api.model.NotificationType;
import cl.gestortareas.api.model.Task.Priority;
import cl.gestortareas.api.model.Task.Status;
import cl.gestortareas.api.repository.CommunityMembershipRepository;

@Service
@Transactional
public class CommunityTaskService {
    public record TaskView(Long id, String description, Status status, Priority priority, LocalDate dueDate,
                           Long assigneeId, String assigneeName, boolean canChangeStatus) {}
    private record Existing(Long assigneeId, String description) {}

    private final JdbcClient jdbc;
    private final CommunityService communities;
    private final CommunityMembershipRepository memberships;
    private final NotificationService notifications;

    public CommunityTaskService(JdbcClient jdbc, CommunityService communities,
                                CommunityMembershipRepository memberships, NotificationService notifications) {
        this.jdbc = jdbc;
        this.communities = communities;
        this.memberships = memberships;
        this.notifications = notifications;
    }

    @Transactional(readOnly = true)
    public List<TaskView> list(Long groupId, String email) {
        var viewer = communities.requireMembership(groupId, email);
        boolean manager = CommunityPostService.MODERATION_ROLES.contains(viewer.getRole());
        return jdbc.sql("""
            SELECT t.id, t.description, t.status, t.priority, t.due_date, t.assignee_id, u.name AS assignee_name
            FROM community_tasks t LEFT JOIN users u ON u.id = t.assignee_id
            WHERE t.community_id = :groupId ORDER BY t.id DESC
            """).param("groupId", groupId).query((rs, row) -> {
                Long assignee = rs.getObject("assignee_id", Long.class);
                return new TaskView(rs.getLong("id"), rs.getString("description"),
                    Status.valueOf(rs.getString("status")), Priority.valueOf(rs.getString("priority")),
                    rs.getObject("due_date", LocalDate.class), assignee, rs.getString("assignee_name"),
                    manager || viewer.getUser().getId().equals(assignee));
            }).list();
    }

    public void create(Long groupId, String email, String description, Priority priority,
                       LocalDate dueDate, Long assigneeId) {
        var actor = communities.requireManager(groupId, email).getUser();
        var assignee = requireAssignee(groupId, assigneeId);
        if (dueDate != null && (dueDate.getYear() < 1 || dueDate.getYear() > 9999)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha límite no es válida");
        }
        jdbc.sql("""
            INSERT INTO community_tasks (community_id, description, priority, due_date, assignee_id)
            VALUES (:groupId, :description, :priority, :dueDate, :assigneeId)
            """).param("groupId", groupId).param("description", description.strip())
            .param("priority", priority.name()).param("dueDate", dueDate, Types.DATE)
            .param("assigneeId", assigneeId, Types.BIGINT).update();
        notifyAssignment(groupId, description.strip(), assignee, actor);
    }

    public void assign(Long groupId, Long taskId, String email, Long assigneeId) {
        var actor = communities.requireManager(groupId, email).getUser();
        var task = lockTask(groupId, taskId);
        var assignee = requireAssignee(groupId, assigneeId);
        if (Objects.equals(task.assigneeId(), assigneeId)) return;
        jdbc.sql("UPDATE community_tasks SET assignee_id = :assigneeId WHERE id = :id")
            .param("id", taskId).param("assigneeId", assigneeId, Types.BIGINT).update();
        notifyAssignment(groupId, task.description(), assignee, actor);
    }

    public void changeStatus(Long groupId, Long taskId, String email, Status status) {
        var member = communities.requireMembership(groupId, email);
        var task = lockTask(groupId, taskId);
        if (!member.getUser().getId().equals(task.assigneeId())
            && !CommunityPostService.MODERATION_ROLES.contains(member.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes cambiar el estado de esta tarea");
        }
        jdbc.sql("UPDATE community_tasks SET status = :status WHERE id = :id")
            .param("id", taskId).param("status", status.name()).update();
    }

    private Existing lockTask(Long groupId, Long taskId) {
        return jdbc.sql("""
            SELECT assignee_id, description FROM community_tasks WHERE community_id = :groupId AND id = :id FOR UPDATE
            """).param("groupId", groupId).param("id", taskId).query(Existing.class).optional()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarea del grupo no encontrada"));
    }

    private AppUser requireAssignee(Long groupId, Long assigneeId) {
        if (assigneeId == null) return null;
        return memberships.findByCommunityIdAndUserId(groupId, assigneeId)
            .map(member -> member.getUser()).filter(AppUser::isActive)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "La persona asignada debe ser miembro activo de este grupo"));
    }

    private void notifyAssignment(Long groupId, String description, AppUser assignee, AppUser actor) {
        if (assignee != null && !assignee.getId().equals(actor.getId())) {
            notifications.create(assignee, actor, NotificationType.TASK, "Te asignaron una tarea de grupo",
                description, "/grupos/" + groupId);
        }
    }
}