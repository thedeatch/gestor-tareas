package cl.gestortareas.api.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Comparator;
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
        if (page < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La página no es válida");
        var member = communities.requireMembership(groupId, email);
        boolean manager = member.getRole() != CommunityRole.MEMBER;
        LocalDate today = LocalDate.now(zone);
        var memories = jdbc.sql("""
            SELECT i.file_id AS image_id, f.original_name AS name, a.id AS album_id, a.title AS album_title,
                   f.created_at AS uploaded_at, i.taken_on, i.taken_year, f.owner_id
            FROM community_album_images i JOIN community_albums a ON a.id = i.album_id
            JOIN stored_files f ON f.id = i.file_id WHERE a.community_id = :groupId
            """).param("groupId", groupId).query(PhotoRow.class).list().stream().map(row ->
                new Memory(row.imageId(), row.name(), row.albumId(), row.albumTitle(),
                    describe(row.uploadedAt().atZoneSameInstant(zone).toLocalDate(), row.takenOn(), row.takenYear(),
                        manager || row.ownerId().equals(member.getUser().getId()), today)))
            .filter(memory -> memory.date().years() >= 1)
            .filter(memory -> !anniversariesOnly || memory.date().anniversary())
            .sorted(Comparator.comparing((Memory memory) -> !memory.date().anniversary())
                .thenComparing(Comparator.comparingInt((Memory memory) -> memory.date().years()).reversed())
                .thenComparing(Memory::imageId, Comparator.reverseOrder())).toList();
        long offset = (long) page * 20;
        List<Memory> items = memories.stream().skip(offset).limit(20).toList();
        return new Page(items, memories.size(), page, 20);
    }
}
