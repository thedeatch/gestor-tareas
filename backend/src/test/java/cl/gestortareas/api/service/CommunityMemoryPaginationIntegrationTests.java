package cl.gestortareas.api.service;

import static org.junit.jupiter.api.Assertions.*;
import java.sql.Types;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.transaction.annotation.Transactional;
import cl.gestortareas.api.model.*;
import cl.gestortareas.api.repository.*;

@SpringBootTest
@Transactional
class CommunityMemoryPaginationIntegrationTests {
    @Autowired CommunityMemoryService service;
    @Autowired JdbcClient jdbc;
    @Autowired AppUserRepository users;
    @Autowired CommunityRepository groups;
    @Autowired CommunityMembershipRepository memberships;
    private record Sample(Long id, LocalDate uploaded, LocalDate taken, Integer year) {}

    @Test
    void databasePaginationMatchesCalendarRulesIncludingLeapDaysAndLocalUploadDates() {
        var user = users.saveAndFlush(new AppUser("Memorias", "memory-pages@test.cl", "hash"));
        var group = groups.saveAndFlush(new Community("Memorias", "memory-pages", "Pruebas", CommunityVisibility.PRIVATE, user));
        memberships.saveAndFlush(new CommunityMembership(group, user, CommunityRole.OWNER));
        var album = new GeneratedKeyHolder();
        jdbc.sql("INSERT INTO community_albums (community_id, title, description) VALUES (:id, 'Historia', '')")
            .param("id", group.getId()).update(album, "id");
        Long albumId = album.getKeyAs(Long.class);
        var samples = new ArrayList<Sample>();
        for (int i = 0; i < 55; i++) {
            LocalDate taken = i % 4 == 0 ? LocalDate.of(2020, 2, 29) :
                i % 4 == 1 ? LocalDate.of(2016 + i % 7, 3, 1) : null;
            Integer year = i % 4 == 2 ? 2010 + i % 10 : null;
            OffsetDateTime uploaded = OffsetDateTime.parse("2020-03-01T01:00:00Z");
            var file = new GeneratedKeyHolder();
            jdbc.sql("""
                INSERT INTO stored_files (owner_id, original_name, content_type, size_bytes, purpose, data, created_at)
                VALUES (:owner, 'Foto', 'image/png', 1, 'COMMUNITY_IMAGE', :bytes, :uploaded)
                """).param("owner", user.getId()).param("bytes", new byte[] {1}).param("uploaded", uploaded).update(file, "id");
            Long id = file.getKeyAs(Long.class);
            jdbc.sql("INSERT INTO community_album_images (album_id, file_id, taken_on, taken_year) VALUES (:album, :id, :taken, :year)")
                .param("album", albumId).param("id", id).param("taken", taken, Types.DATE).param("year", year, Types.INTEGER).update();
            samples.add(new Sample(id, uploaded.atZoneSameInstant(ZoneId.of("America/Santiago")).toLocalDate(), taken, year));
        }
        for (LocalDate today : new LocalDate[] { LocalDate.of(2021, 2, 28), LocalDate.of(2024, 2, 28),
                LocalDate.of(2024, 2, 29), LocalDate.of(2026, 3, 1) }) {
            for (boolean onlyAnniversaries : new boolean[] { false, true }) {
                var expected = samples.stream().map(sample -> new CommunityMemoryService.Memory(sample.id(), "Foto", albumId, "Historia",
                    CommunityMemoryService.describe(sample.uploaded(), sample.taken(), sample.year(), true, today)))
                    .filter(memory -> memory.date().years() >= 1)
                    .filter(memory -> !onlyAnniversaries || memory.date().anniversary())
                    .sorted(Comparator.comparing((CommunityMemoryService.Memory memory) -> !memory.date().anniversary())
                        .thenComparing(Comparator.comparingInt((CommunityMemoryService.Memory memory) -> memory.date().years()).reversed())
                        .thenComparing(CommunityMemoryService.Memory::imageId, Comparator.reverseOrder())).toList();
                for (int page = 0; page < 4; page++) {
                    var actual = service.findAllAt(group.getId(), user.getEmail(), onlyAnniversaries, page, today);
                    assertEquals(expected.size(), actual.total());
                    assertEquals(expected.stream().skip(page * 20L).limit(20).toList(), actual.items(), "Calendar parity for " + today);
                }
            }
        }
    }
}
