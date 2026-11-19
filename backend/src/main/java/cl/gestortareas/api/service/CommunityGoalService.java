package cl.gestortareas.api.service;

import java.sql.Types;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import cl.gestortareas.api.model.Task.Status;
import cl.gestortareas.api.service.CommunityTaskService.TaskView;

@Service
@Transactional
public class CommunityGoalService {
    public record Goal(Long id, String title, String description, LocalDate dueDate, OffsetDateTime createdAt,
                       String authorName, List<Long> taskIds, long totalTasks, long completedTasks, int progress) {}
    public record Overview(List<Goal> goals, List<TaskView> tasks, boolean canManage) {}
    private record GoalRow(Long id, String title, String description, LocalDate dueDate,
                           OffsetDateTime createdAt, String authorName) {}
    private record Link(Long goalId, Long taskId) {}
    private final JdbcClient jdbc;
    private final CommunityService communities;
    private final CommunityTaskService tasks;

    public CommunityGoalService(JdbcClient jdbc, CommunityService communities, CommunityTaskService tasks) {
        this.jdbc = jdbc;
        this.communities = communities;
        this.tasks = tasks;
    }

    @Transactional(readOnly = true)
    public Overview findAll(Long groupId, String email) {
        var viewer = communities.requireMembership(groupId, email);
        List<TaskView> available = tasks.list(groupId, email);
        Map<Long, TaskView> byId = available.stream().collect(Collectors.toMap(TaskView::id, task -> task));
        var links = jdbc.sql("SELECT goal_id, task_id FROM community_goal_tasks WHERE community_id = :groupId ORDER BY task_id")
            .param("groupId", groupId).query(Link.class).list().stream()
            .collect(Collectors.groupingBy(Link::goalId, Collectors.mapping(Link::taskId, Collectors.toList())));
        var rows = jdbc.sql("""
            SELECT g.id, g.title, g.description, g.due_date, g.created_at, u.name AS author_name
            FROM community_goals g LEFT JOIN users u ON u.id = g.created_by
            WHERE g.community_id = :groupId ORDER BY g.id DESC
            """).param("groupId", groupId).query(GoalRow.class).list();
        var goals = rows.stream().map(row -> {
            List<Long> ids = links.getOrDefault(row.id(), List.of()).stream().filter(byId::containsKey).toList();
            long completed = ids.stream().filter(id -> byId.get(id).status() == Status.COMPLETED).count();
            int progress = ids.isEmpty() ? 0 : (int) Math.round(completed * 100.0 / ids.size());
            return new Goal(row.id(), row.title(), row.description(), row.dueDate(), row.createdAt(),
                row.authorName(), ids, ids.size(), completed, progress);
        }).toList();
        return new Overview(goals, available, CommunityPostService.MODERATION_ROLES.contains(viewer.getRole()));
    }

    public Overview create(Long groupId, String email, String title, String description,
                           LocalDate dueDate, List<Long> taskIds) {
        var actor = communities.requireManager(groupId, email);
        validateDate(dueDate);
        List<Long> ids = requireTasks(groupId, taskIds);
        var key = new GeneratedKeyHolder();
        jdbc.sql("""
            INSERT INTO community_goals (community_id, created_by, title, description, due_date)
            VALUES (:groupId, :userId, :title, :description, :dueDate)
            """).param("groupId", groupId).param("userId", actor.getUser().getId())
            .param("title", title.strip()).param("description", description.strip())
            .param("dueDate", dueDate, Types.DATE).update(key, "id");
        replaceLinks(groupId, key.getKeyAs(Long.class), ids);
        return findAll(groupId, email);
    }

    public Overview update(Long groupId, Long goalId, String email, String title, String description,
                           LocalDate dueDate, List<Long> taskIds) {
        communities.requireManager(groupId, email);
        lock(groupId, goalId);
        validateDate(dueDate);
        List<Long> ids = requireTasks(groupId, taskIds);
        jdbc.sql("UPDATE community_goals SET title = :title, description = :description, due_date = :dueDate WHERE id = :id")
            .param("title", title.strip()).param("description", description.strip())
            .param("dueDate", dueDate, Types.DATE).param("id", goalId).update();
        replaceLinks(groupId, goalId, ids);
        return findAll(groupId, email);
    }

    public Overview delete(Long groupId, Long goalId, String email) {
        communities.requireManager(groupId, email);
        lock(groupId, goalId);
        jdbc.sql("DELETE FROM community_goals WHERE id = :id").param("id", goalId).update();
        return findAll(groupId, email);
    }

    private void lock(Long groupId, Long goalId) {
        jdbc.sql("SELECT id FROM community_goals WHERE community_id = :groupId AND id = :id FOR UPDATE")
            .param("groupId", groupId).param("id", goalId).query(Long.class).optional()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Objetivo no encontrado"));
    }

    private List<Long> requireTasks(Long groupId, List<Long> taskIds) {
        List<Long> ids = taskIds.stream().distinct().sorted().toList();
        if (ids.isEmpty()) return ids;
        var found = jdbc.sql("SELECT id FROM community_tasks WHERE community_id = :groupId AND id IN (:ids) ORDER BY id FOR UPDATE")
            .param("groupId", groupId).param("ids", ids).query(Long.class).list();
        if (found.size() != ids.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Las tareas vinculadas deben pertenecer a este grupo");
        }
        return ids;
    }

    private void replaceLinks(Long groupId, Long goalId, List<Long> taskIds) {
        jdbc.sql("DELETE FROM community_goal_tasks WHERE goal_id = :id").param("id", goalId).update();
        for (Long taskId : taskIds) {
            jdbc.sql("INSERT INTO community_goal_tasks (community_id, goal_id, task_id) VALUES (:groupId, :goalId, :taskId)")
                .param("groupId", groupId).param("goalId", goalId).param("taskId", taskId).update();
        }
    }

    private void validateDate(LocalDate dueDate) {
        if (dueDate != null && (dueDate.getYear() < 1 || dueDate.getYear() > 9999)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha límite no es válida");
        }
    }
}
