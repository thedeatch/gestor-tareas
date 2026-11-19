package cl.gestortareas.api.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import cl.gestortareas.api.model.CommunityRole;

@Service
public class CommunityMemoryService {
    public record PhotoDate(LocalDate uploadedOn, LocalDate takenOn, Integer takenYear,
                            int years, boolean anniversary, boolean canEdit) {}
    public record Memory(Long imageId, String name, Long albumId, String albumTitle, PhotoDate date) {}
    public record Page(List<Memory> items, long total, int page, int size) {}
    private record PhotoRow(Long imageId, String name, Long albumId, String albumTitle,
                            OffsetDateTime uploadedAt, LocalDate takenOn, Integer takenYear, Long ownerId) {}
    private final JdbcClient jdbc;
    private final CommunityService communities;
    private final ZoneId zone;

    public CommunityMemoryService(JdbcClient jdbc, CommunityService communities,
                                  @Value("${app.memories-zone:America/Santiago}") String zone) {
        this.jdbc = jdbc;
        this.communities = communities;
        this.zone = ZoneId.of(zone);
    }

    public PhotoDate describe(Instant uploadedAt, LocalDate takenOn, Integer takenYear, boolean canEdit) {
        return describe(uploadedAt.atZone(zone).toLocalDate(), takenOn, takenYear, canEdit, LocalDate.now(zone));
    }

    static PhotoDate describe(LocalDate uploadedOn, LocalDate takenOn, Integer takenYear,
                              boolean canEdit, LocalDate today) {
        if (takenYear != null) {
            return new PhotoDate(uploadedOn, null, takenYear, today.getYear() - takenYear, false, canEdit);
        }
        LocalDate reference = takenOn == null ? uploadedOn : takenOn;
        int years = today.getYear() - reference.getYear();
        if (reference.plusYears(years).isAfter(today)) years--;
        boolean anniversary = years > 0 && reference.plusYears(years).equals(today);
        return new PhotoDate(uploadedOn, takenOn, null, Math.max(0, years), anniversary, canEdit);
    }

    public void validate(LocalDate takenOn, Integer takenYear) {
        LocalDate today = LocalDate.now(zone);
        if (takenOn != null && takenYear != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indica la fecha completa o solo el año");
        }
        if (takenOn != null && (takenOn.getYear() < 1 || takenOn.getYear() > 9999 || takenOn.isAfter(today))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha de la foto debe ser válida y no puede ser futura");
        }
        if (takenYear != null && (takenYear < 1 || takenYear > today.getYear())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El año de la foto debe ser válido y no puede ser futuro");
        }
    }

    @Transactional(readOnly = true)
    public Page findAll(Long groupId, String email, boolean anniversariesOnly, int page) {
        return findAllAt(groupId, email, anniversariesOnly, page, LocalDate.now(zone));
    }

    Page findAllAt(Long groupId, String email, boolean anniversariesOnly, int page, LocalDate today) {
        if (page < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La página no es válida");
        var member = communities.requireMembership(groupId, email);
        boolean manager = member.getRole() != CommunityRole.MEMBER;
        var params = Map.<String, Object>of("groupId", groupId, "zone", zone.getId(),
            "year", today.getYear(), "month", today.getMonthValue(), "day", today.getDayOfMonth(),
            "leap", today.isLeapYear(), "anniversariesOnly", anniversariesOnly, "offset", (long) page * 20);
        String query = """
            WITH photo_dates AS (
                SELECT i.file_id AS image_id, f.original_name AS name, a.id AS album_id, a.title AS album_title,
                       f.created_at AS uploaded_at, i.taken_on, i.taken_year, f.owner_id,
                       COALESCE(i.taken_on, CAST(f.created_at AT TIME ZONE :zone AS DATE)) AS reference_date
                FROM community_album_images i JOIN community_albums a ON a.id = i.album_id
                JOIN stored_files f ON f.id = i.file_id WHERE a.community_id = :groupId
            ), calendar_dates AS (
                SELECT photo_dates.*, EXTRACT(YEAR FROM reference_date) AS reference_year,
                       EXTRACT(MONTH FROM reference_date) AS reference_month,
                       CASE WHEN EXTRACT(MONTH FROM reference_date) = 2
                            AND EXTRACT(DAY FROM reference_date) = 29 AND :leap = FALSE THEN 28
                            ELSE EXTRACT(DAY FROM reference_date) END AS reference_day
                FROM photo_dates
            ), memories AS (
                SELECT calendar_dates.*,
                       CASE WHEN taken_year IS NOT NULL THEN :year - taken_year
                            ELSE :year - reference_year - CASE
                                WHEN reference_month > :month OR (reference_month = :month AND reference_day > :day)
                                THEN 1 ELSE 0 END END AS memory_years,
                       (taken_year IS NULL AND reference_month = :month AND reference_day = :day) AS anniversary
                FROM calendar_dates
            )
            """;
        String filtered = " FROM memories WHERE memory_years >= 1 AND (:anniversariesOnly = FALSE OR anniversary = TRUE)";
        long total = jdbc.sql(query + "SELECT COUNT(*)" + filtered).params(params).query(Long.class).single();
        var items = jdbc.sql(query + "SELECT *" + filtered
                + " ORDER BY anniversary DESC, memory_years DESC, image_id DESC LIMIT 20 OFFSET :offset")
            .params(params).query(PhotoRow.class).list().stream().map(row ->
                new Memory(row.imageId(), row.name(), row.albumId(), row.albumTitle(),
                    describe(row.uploadedAt().atZoneSameInstant(zone).toLocalDate(), row.takenOn(), row.takenYear(),
                        manager || row.ownerId().equals(member.getUser().getId()), today))).toList();
        return new Page(items, total, page, 20);
    }
}