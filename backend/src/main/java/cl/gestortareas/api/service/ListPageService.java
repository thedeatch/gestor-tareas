package cl.gestortareas.api.service;

import java.time.LocalDate;
import java.text.Normalizer;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import cl.gestortareas.api.dto.CommunityPostResponse;
import cl.gestortareas.api.model.Task;
import cl.gestortareas.api.repository.*;
import cl.gestortareas.api.service.CommunityAlbumService.Album;
import cl.gestortareas.api.service.CommunityEventService.Event;
import cl.gestortareas.api.service.CommunityTaskService.TaskView;

@Service
@Transactional(readOnly = true)
public class ListPageService {
    public record Page<T>(List<T> items, long total, int page, int size, long allTotal) {}
    public record Assignee(Long id, String name) {}
    public record TaskFilter(String q, String status, String priority, String due, String project,
                             String category, String assignee,
                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate today) {}
    private record IdPage(List<Long> ids, long total, int page, int size, long allTotal) {
        <T> Page<T> with(List<T> items) { return new Page<>(items, total, page, size, allTotal); }
    }
    private final JdbcClient jdbc;
    private final CommunityService communities;
    private final CommunityPostService posts;
    private final CommunityPostRepository postRepository;
    private final CommunityPostReactionRepository reactions;
    private final TaskRepository tasks;
    private final AppUserRepository users;

    public ListPageService(JdbcClient jdbc, CommunityService communities, CommunityPostService posts,
                           CommunityPostRepository postRepository, CommunityPostReactionRepository reactions,
                           TaskRepository tasks, AppUserRepository users) {
        this.jdbc = jdbc; this.communities = communities; this.posts = posts;
        this.postRepository = postRepository; this.reactions = reactions; this.tasks = tasks; this.users = users;
    }

    public Page<CommunityPostResponse> posts(Long group, String email, int page, Long target) {
        var member = communities.requireMembership(group, email);
        var ids = ids("p.id", " FROM community_posts p WHERE p.community_id = :groupId", "",
            "p.created_at DESC, p.id DESC", page, 20, target, new HashMap<>(Map.of("groupId", group)));
        var summaries = reactions.summarize(ids.ids(), member.getUser().getId());
        var byId = new HashMap<Long, cl.gestortareas.api.model.CommunityPost>();
        postRepository.findAllById(ids.ids()).forEach(post -> byId.put(post.getId(), post));
        return ids.with(ids.ids().stream().filter(byId::containsKey).map(id -> posts.toResponse(byId.get(id), member,
            summaries.getOrDefault(id, List.of()))).toList());
    }

    public Page<Album> albums(Long group, String email, int page, Long target) {
        communities.requireMembership(group, email);
        var ids = ids("a.id", " FROM community_albums a WHERE a.community_id = :groupId", "", "a.id DESC",
            page, 12, target, new HashMap<>(Map.of("groupId", group)));
        if (ids.ids().isEmpty()) return ids.with(List.of());
        var rows = jdbc.sql("""
            SELECT a.id, a.title, a.description, COUNT(i.file_id) AS image_count, MAX(i.file_id) AS cover_image_id
            FROM community_albums a LEFT JOIN community_album_images i ON i.album_id = a.id
            WHERE a.community_id = :groupId AND a.id IN (:ids)
            GROUP BY a.id, a.title, a.description ORDER BY a.id DESC
            """).param("groupId", group).param("ids", ids.ids()).query(Album.class).list();
        return ids.with(rows);
    }

    public Page<Event> events(Long group, String email, int page, Long target) {
        var viewer = communities.requireMembership(group, email);
        var ids = ids("e.id", " FROM community_events e WHERE e.community_id = :groupId", "",
            "CASE WHEN e.starts_at >= CURRENT_TIMESTAMP THEN 0 ELSE 1 END, e.starts_at, e.id", page, 20, target,
            new HashMap<>(Map.of("groupId", group)));
        if (ids.ids().isEmpty()) return ids.with(List.of());
        var rows = jdbc.sql("""
            SELECT e.id, e.title, e.description, e.starts_at, COUNT(r.user_id) AS participant_count,
                   COUNT(CASE WHEN r.user_id = :userId THEN 1 END) > 0 AS registered
            FROM community_events e LEFT JOIN community_event_registrations r ON r.event_id = e.id
            WHERE e.community_id = :groupId AND e.id IN (:ids)
            GROUP BY e.id, e.title, e.description, e.starts_at
            ORDER BY CASE WHEN e.starts_at >= CURRENT_TIMESTAMP THEN 0 ELSE 1 END, e.starts_at, e.id
            """).param("groupId", group).param("ids", ids.ids()).param("userId", viewer.getUser().getId()).query(Event.class).list();
        return ids.with(rows);
    }

    public Page<Task> personalTasks(String email, int page, TaskFilter filter) {
        Long owner = users.findByEmailIgnoreCase(email).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no disponible")).getId();
        var params = new HashMap<String, Object>(Map.of("owner", owner));
        String where = taskWhere(filter, false, params);
        var ids = ids("t.id", " FROM tasks t WHERE t.user_id = :owner", where, "t.id DESC", page, 20, null, params);
        var byId = new HashMap<Long, Task>();
        tasks.findAllById(ids.ids()).forEach(task -> byId.put(task.getId(), task));
        return ids.with(ids.ids().stream().filter(byId::containsKey).map(byId::get).toList());
    }

    public Page<TaskView> groupTasks(Long group, String email, int page, Long target, TaskFilter filter) {
        var viewer = communities.requireMembership(group, email);
        var params = new HashMap<String, Object>(Map.of("groupId", group));
        String where = taskWhere(filter, true, params);
        var ids = ids("t.id", " FROM community_tasks t LEFT JOIN users u ON u.id = t.assignee_id WHERE t.community_id = :groupId",
            where, "t.id DESC", page, 20, target, params);
        if (ids.ids().isEmpty()) return ids.with(List.of());
        boolean manager = CommunityPostService.MODERATION_ROLES.contains(viewer.getRole());
        var rows = jdbc.sql("""
            SELECT t.id, t.description, t.status, t.priority, t.due_date, t.assignee_id, u.name AS assignee_name
            FROM community_tasks t LEFT JOIN users u ON u.id = t.assignee_id
            WHERE t.community_id = :groupId AND t.id IN (:ids) ORDER BY t.id DESC
            """).param("groupId", group).param("ids", ids.ids()).query((rs, row) -> {
                Long assignee = rs.getObject("assignee_id", Long.class);
                return new TaskView(rs.getLong("id"), rs.getString("description"), Task.Status.valueOf(rs.getString("status")),
                    Task.Priority.valueOf(rs.getString("priority")), rs.getObject("due_date", LocalDate.class),
                    assignee, rs.getString("assignee_name"), manager || viewer.getUser().getId().equals(assignee));
            }).list();
        return ids.with(rows);
    }

    public List<Assignee> assignees(Long group, String email) {
        communities.requireMembership(group, email);
        return jdbc.sql("""
            SELECT DISTINCT u.id, u.name FROM community_tasks t JOIN users u ON u.id = t.assignee_id
            WHERE t.community_id = :groupId ORDER BY u.name, u.id
            """).param("groupId", group).query(Assignee.class).list();
    }

    private IdPage ids(String id, String base, String where, String order, int page, int size, Long target, Map<String, Object> params) {
        if (page < 0) throw bad("La página no es válida");
        long all = jdbc.sql("SELECT COUNT(*)" + base).params(params).query(Long.class).single();
        long total = where.isEmpty() ? all : jdbc.sql("SELECT COUNT(*)" + base + where).params(params).query(Long.class).single();
        if (target != null) {
            params.put("target", target);
            long position = jdbc.sql("SELECT position FROM (SELECT " + id + " AS id, ROW_NUMBER() OVER (ORDER BY " + order
                + ") - 1 AS position" + base + where + ") ranked WHERE id = :target")
                .params(params).query(Long.class).optional().orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.NOT_FOUND, "El elemento no está disponible en este listado"));
            page = (int) (position / size);
        }
        if (total > 0 && (long) page * size >= total) page = (int) ((total - 1) / size);
        if (total == 0) return new IdPage(List.of(), 0, 0, size, all);
        params.put("offset", (long) page * size); params.put("limit", size);
        var result = jdbc.sql("SELECT " + id + base + where + " ORDER BY " + order + " LIMIT :limit OFFSET :offset")
            .params(params).query(Long.class).list();
        return new IdPage(result, total, page, size, all);
    }

    private String taskWhere(TaskFilter filter, boolean group, Map<String, Object> params) {
        StringBuilder sql = new StringBuilder();
        String status = value(filter.status()), priority = value(filter.priority()), due = value(filter.due());
        if (!status.isEmpty()) {
            try { Task.Status.valueOf(status); } catch (IllegalArgumentException error) { throw bad("Estado no válido"); }
            sql.append(" AND t.status = :status"); params.put("status", status);
        }
        if (!priority.isEmpty()) {
            try { Task.Priority.valueOf(priority); } catch (IllegalArgumentException error) { throw bad("Prioridad no válida"); }
            sql.append(" AND t.priority = :priority"); params.put("priority", priority);
        }
        String q = value(filter.q()).strip();
        if (q.length() > 200) throw bad("El texto de búsqueda es demasiado largo");
        if (!q.isEmpty()) {
            String text = group ? "t.description || ' ' || COALESCE(u.name, '')" : "t.description || ' ' || t.tags_text";
            String normalized = "TRANSLATE(LOWER(" + text + "), 'áàäâãåéèëêíìïîóòöôõúùüûñç', 'aaaaaaeeeeiiiiooooouuuunc')";
            for (String mark : List.of("\u0300", "\u0301", "\u0302", "\u0303", "\u0308", "\u030A", "\u0327")) normalized = "REPLACE(" + normalized + ", '" + mark + "', '')";
            var words = Normalizer.normalize(q, Normalizer.Form.NFD).replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT).split("\\s+");
            for (int i = 0; i < words.length; i++) {
                sql.append(" AND ").append(normalized).append(" LIKE :word").append(i).append(" ESCAPE '!'");
                params.put("word" + i, "%" + words[i].replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%");
            }
        }
        if (group) selection(sql, params, "assignee", "t.assignee_id", filter.assignee());
        else { selection(sql, params, "project", "t.project_id", filter.project()); selection(sql, params, "category", "t.category_id", filter.category()); }
        if (!due.isEmpty()) {
            LocalDate today = filter.today() == null ? LocalDate.now(java.time.ZoneId.of("America/Santiago")) : filter.today();
            if (today.getYear() < 1 || today.getYear() > 9999) throw bad("La fecha no es válida");
            params.put("today", today); params.put("weekEnd", today.plusDays(6));
            sql.append(switch (due) {
                case "NONE" -> " AND t.due_date IS NULL";
                case "OVERDUE" -> " AND t.due_date < :today AND t.status <> 'COMPLETED'";
                case "TODAY" -> " AND t.due_date = :today";
                case "WEEK" -> " AND t.due_date BETWEEN :today AND :weekEnd";
                default -> throw bad("Filtro de fecha no válido");
            });
        }
        return sql.toString();
    }

    private void selection(StringBuilder sql, Map<String, Object> params, String key, String column, String raw) {
        String selected = value(raw);
        if (selected.isEmpty()) return;
        if (selected.equals("NONE")) { sql.append(" AND ").append(column).append(" IS NULL"); return; }
        try {
            long id = Long.parseLong(selected); if (id < 1) throw new NumberFormatException();
            params.put(key, id); sql.append(" AND ").append(column).append(" = :").append(key);
        } catch (NumberFormatException error) { throw bad("Selección no válida"); }
    }
    private String value(String value) { return value == null ? "" : value; }
    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
}
