package cl.gestortareas.api.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class SubtaskService {
    public record Subtask(Long id, String description, boolean completed) {}

    private final JdbcClient jdbc;
    private final TaskService personal;
    private final CommunityTaskService group;

    public SubtaskService(JdbcClient jdbc, TaskService personal, CommunityTaskService group) {
        this.jdbc = jdbc;
        this.personal = personal;
        this.group = group;
    }

    @Transactional(readOnly = true)
    public List<Subtask> list(Long groupId, Long taskId, String email) {
        String column = authorize(groupId, taskId, email, false);
        return jdbc.sql("SELECT id, description, completed FROM task_subtasks WHERE " + column
                + " = :taskId ORDER BY id")
            .param("taskId", taskId).query(Subtask.class).list();
    }

    public void create(Long groupId, Long taskId, String email, String description) {
        String column = authorize(groupId, taskId, email, true);
        jdbc.sql("INSERT INTO task_subtasks (" + column + ", description) VALUES (:taskId, :description)")
            .param("taskId", taskId).param("description", description.strip()).update();
    }

    public void update(Long groupId, Long taskId, Long id, String email, String description, boolean completed) {
        String column = authorize(groupId, taskId, email, true);
        requireChanged(jdbc.sql("UPDATE task_subtasks SET description = :description, completed = :completed WHERE "
                + column + " = :taskId AND id = :id")
            .param("taskId", taskId).param("id", id).param("description", description.strip())
            .param("completed", completed).update());
    }

    public void delete(Long groupId, Long taskId, Long id, String email) {
        String column = authorize(groupId, taskId, email, true);
        requireChanged(jdbc.sql("DELETE FROM task_subtasks WHERE " + column + " = :taskId AND id = :id")
            .param("taskId", taskId).param("id", id).update());
    }

    private String authorize(Long groupId, Long taskId, String email, boolean write) {
        if (groupId == null) {
            personal.findById(taskId, email);
            return "task_id";
        }
        group.requireSubtaskAccess(groupId, taskId, email, write);
        return "community_task_id";
    }

    private void requireChanged(int changed) {
        if (changed == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Subtarea no encontrada");
        }
    }
}