package cl.gestortareas.api.service;

import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import cl.gestortareas.api.model.Task;
import cl.gestortareas.api.repository.AppUserRepository;

@Service
@Transactional
public class TaskOrganizationService {
    public enum Kind { PROJECT, CATEGORY }
    public record Item(Long id, Kind kind, String name) {}

    private final JdbcClient jdbc;
    private final AppUserRepository users;
    private final TaskService tasks;

    public TaskOrganizationService(JdbcClient jdbc, AppUserRepository users, TaskService tasks) {
        this.jdbc = jdbc;
        this.users = users;
        this.tasks = tasks;
    }

    @Transactional(readOnly = true)
    public List<Item> list(String email) {
        return jdbc.sql("""
            SELECT id, kind, name FROM task_organization WHERE user_id = :owner ORDER BY kind, name_key
            """).param("owner", userId(email)).query(Item.class).list();
    }

    public void create(String email, Kind kind, String name) {
        Long owner = lockOwner(email);
        String normalized = name.strip();
        String key = normalized.toLowerCase(Locale.ROOT);
        requireUnique(owner, kind, key, null);
        jdbc.sql("""
            INSERT INTO task_organization (user_id, kind, name, name_key)
            VALUES (:owner, :kind, :name, :key)
            """).param("owner", owner).param("kind", kind.name())
            .param("name", normalized).param("key", key).update();
    }

    public void rename(String email, Long id, String name) {
        Long owner = lockOwner(email);
        Item item = requireItem(owner, id);
        String normalized = name.strip();
        String key = normalized.toLowerCase(Locale.ROOT);
        requireUnique(owner, item.kind(), key, id);
        jdbc.sql("UPDATE task_organization SET name = :name, name_key = :key WHERE id = :id")
            .param("id", id).param("name", normalized).param("key", key).update();
    }

    public void delete(String email, Long id) {
        Long owner = lockOwner(email);
        int deleted = jdbc.sql("DELETE FROM task_organization WHERE user_id = :owner AND id = :id")
            .param("owner", owner).param("id", id).update();
        if (deleted == 0) throw missing();
    }

    public Task classify(String email, Long taskId, Long projectId, Long categoryId) {
        Long owner = lockOwner(email);
        Task task = tasks.findById(taskId, email);
        requireKind(owner, projectId, Kind.PROJECT);
        requireKind(owner, categoryId, Kind.CATEGORY);
        task.classify(projectId, categoryId);
        return task;
    }

    private void requireKind(Long owner, Long id, Kind kind) {
        if (id != null && requireItem(owner, id).kind() != kind) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La clasificación no corresponde al tipo indicado");
        }
    }

    private Item requireItem(Long owner, Long id) {
        return jdbc.sql("SELECT id, kind, name FROM task_organization WHERE user_id = :owner AND id = :id")
            .param("owner", owner).param("id", id).query(Item.class).optional().orElseThrow(this::missing);
    }

    private void requireUnique(Long owner, Kind kind, String key, Long allowedId) {
        var existing = jdbc.sql("""
            SELECT id FROM task_organization WHERE user_id = :owner AND kind = :kind AND name_key = :key
            """).param("owner", owner).param("kind", kind.name()).param("key", key)
            .query(Long.class).optional();
        if (existing.isPresent() && !existing.get().equals(allowedId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un elemento de este tipo con ese nombre");
        }
    }

    private Long lockOwner(String email) {
        Long id = userId(email);
        return jdbc.sql("SELECT id FROM users WHERE id = :id FOR UPDATE")
            .param("id", id).query(Long.class).single();
    }

    private Long userId(String email) {
        return users.findByEmailIgnoreCase(email).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado")).getId();
    }

    private ResponseStatusException missing() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto o categoría no encontrado");
    }
}