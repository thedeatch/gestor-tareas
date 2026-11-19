package cl.gestortareas.api.service;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CommunitySearchService {
    public enum Kind { POST, TASK, EVENT, ALBUM }
    public enum Filter { ALL, POST, TASK, EVENT, ALBUM }
    public record Result(Kind kind, Long id, String title, String excerpt) {}
    public record Page(String query, List<Result> items, long total, int page, int size) {}
    private static final int PAGE_SIZE = 20;
    private static final String NORMALIZED =
        "TRANSLATE(LOWER(%s), 'áàäâãåéèëêíìïîóòöôõúùüûñç', 'aaaaaaeeeeiiiiooooouuuunc')";
    private static final String CONTENT = """
        SELECT 'POST' AS kind, p.id,
               COALESCE(NULLIF(p.title, ''), 'Publicación de ' || u.name) AS title,
               p.content AS excerpt,
               COALESCE(p.title, '') || ' ' || p.content || ' ' || u.name AS search_text
        FROM community_posts p JOIN users u ON u.id = p.author_user_id WHERE p.community_id = :groupId
        UNION ALL
        SELECT 'TASK' AS kind, t.id, t.description AS title,
               CASE WHEN u.name IS NULL THEN 'Sin responsable' ELSE 'Asignada a ' || u.name END AS excerpt,
               t.description || ' ' || COALESCE(u.name, '') AS search_text
        FROM community_tasks t LEFT JOIN users u ON u.id = t.assignee_id WHERE t.community_id = :groupId
        UNION ALL
        SELECT 'EVENT' AS kind, e.id, e.title, e.description AS excerpt,
               e.title || ' ' || e.description AS search_text
        FROM community_events e WHERE e.community_id = :groupId
        UNION ALL
        SELECT 'ALBUM' AS kind, a.id, a.title, a.description AS excerpt,
               a.title || ' ' || a.description AS search_text
        FROM community_albums a WHERE a.community_id = :groupId
        """;
    private final JdbcClient jdbc;
    private final CommunityService communities;

    public CommunitySearchService(JdbcClient jdbc, CommunityService communities) {
        this.jdbc = jdbc;
        this.communities = communities;
    }

    @Transactional(readOnly = true)
    public Page search(Long groupId, String email, String query, Filter kind, int page) {
        communities.requireMembership(groupId, email);
        String text = query.strip();
        if (text.length() > 200 || page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Revisa el texto de búsqueda y la página solicitada");
        }
        if (text.isEmpty()) return new Page("", List.of(), 0, page, PAGE_SIZE);
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
        var params = new HashMap<String, Object>();
        params.put("groupId", groupId);
        params.put("kind", kind.name());
        params.put("exact", normalized);
        params.put("offset", (long) page * PAGE_SIZE);
        StringBuilder where = new StringBuilder(" FROM (" + CONTENT + ") content WHERE (:kind = 'ALL' OR kind = :kind)");
        String[] words = normalized.split("\\s+");
        for (int i = 0; i < words.length; i++) {
            String key = "word" + i;
            String literal = words[i].replace("!", "!!").replace("%", "!%").replace("_", "!_");
            params.put(key, "%" + literal + "%");
            where.append(" AND ").append(normalizedSql("search_text")).append(" LIKE :").append(key).append(" ESCAPE '!'");
        }
        long total = jdbc.sql("SELECT COUNT(*)" + where).params(params).query(Long.class).single();
        var items = jdbc.sql("SELECT kind, id, title, excerpt" + where
                + " ORDER BY CASE WHEN " + normalizedSql("title") + " = :exact THEN 0 ELSE 1 END, kind, id DESC"
                + " LIMIT " + PAGE_SIZE + " OFFSET :offset")
            .params(params).query(Result.class).list();
        return new Page(text, items, total, page, PAGE_SIZE);
    }

    private String normalizedSql(String column) {
        String expression = NORMALIZED.formatted(column);
        // Remove decomposed accents using operations supported by PostgreSQL and H2.
        for (String mark : List.of("\u0300", "\u0301", "\u0302", "\u0303", "\u0308", "\u030A", "\u0327")) {
            expression = "REPLACE(" + expression + ", '" + mark + "', '')";
        }
        return expression;
    }
}
