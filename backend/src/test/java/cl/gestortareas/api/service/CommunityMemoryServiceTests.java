package cl.gestortareas.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class CommunityMemoryServiceTests {
    @Test
    void exactDatesCountCompletedYearsAndIdentifyTheAnniversary() {
        LocalDate taken = LocalDate.of(2016, 11, 18);
        LocalDate uploaded = LocalDate.of(2026, 1, 1);
        assertEquals(9, CommunityMemoryService.describe(uploaded, taken, null, true,
            LocalDate.of(2026, 11, 17)).years());
        var anniversary = CommunityMemoryService.describe(uploaded, taken, null, true, LocalDate.of(2026, 11, 18));
        assertEquals(10, anniversary.years());
        assertTrue(anniversary.anniversary());
        assertFalse(CommunityMemoryService.describe(uploaded, taken, null, true,
            LocalDate.of(2026, 11, 19)).anniversary());
    }

    @Test
    void yearOnlyUsesCalendarYearsAndDefaultUsesTheUploadDate() {
        var yearOnly = CommunityMemoryService.describe(LocalDate.of(2026, 11, 18), null, 2016,
            true, LocalDate.of(2027, 1, 1));
        assertEquals(11, yearOnly.years());
        assertFalse(yearOnly.anniversary());
        var fallback = CommunityMemoryService.describe(LocalDate.of(2020, 11, 18), null, null,
            false, LocalDate.of(2026, 11, 18));
        assertEquals(6, fallback.years());
        assertTrue(fallback.anniversary());
    }

    @Test
    void leapDayPhotosHaveTheirAnniversaryOnFebruary28InNonLeapYears() {
        LocalDate taken = LocalDate.of(2020, 2, 29);
        var first = CommunityMemoryService.describe(taken, taken, null, false, LocalDate.of(2021, 2, 28));
        assertEquals(1, first.years());
        assertTrue(first.anniversary());
        var leap = CommunityMemoryService.describe(taken, taken, null, false, LocalDate.of(2024, 2, 29));
        assertEquals(4, leap.years());
        assertTrue(leap.anniversary());
    }
}
