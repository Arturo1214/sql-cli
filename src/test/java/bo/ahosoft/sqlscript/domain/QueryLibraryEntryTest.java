package bo.ahosoft.sqlscript.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import org.junit.Test;

public class QueryLibraryEntryTest {

    @Test
    public void storesImmutableQueryMetadataAndSqlText() {
        Instant created = Instant.parse("2026-06-09T10:00:00Z");
        Instant updated = Instant.parse("2026-06-09T10:05:00Z");
        ArrayList<String> tags = new ArrayList<>(Arrays.asList("support", "billing"));

        QueryLibraryEntry entry = new QueryLibraryEntry(
            "daily-support-report",
            "Daily Support Report",
            "select * from tickets",
            "Open ticket count",
            tags,
            true,
            "PROD",
            "prod-support",
            created,
            updated
        );
        tags.add("mutated");

        assertEquals("daily-support-report", entry.id());
        assertEquals("Daily Support Report", entry.name());
        assertEquals("select * from tickets", entry.sql());
        assertEquals("Open ticket count", entry.description());
        assertEquals(Arrays.asList("support", "billing"), entry.tags());
        assertTrue(entry.favorite());
        assertEquals("PROD", entry.environmentScope());
        assertEquals("prod-support", entry.connectionScope());
        assertEquals(created, entry.createdAt());
        assertEquals(updated, entry.updatedAt());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void preventsTagMutationThroughAccessor() {
        QueryLibraryEntry entry = new QueryLibraryEntry(
            "incident-review",
            "Incident Review",
            "select 1",
            "",
            Arrays.asList("incident"),
            false,
            "",
            "",
            Instant.parse("2026-06-09T11:00:00Z"),
            Instant.parse("2026-06-09T11:00:00Z")
        );

        entry.tags().add("mutated");
    }

    @Test
    public void normalizesNullOptionalFieldsForLegacyCompatibility() {
        Instant now = Instant.parse("2026-06-09T12:00:00Z");

        QueryLibraryEntry entry = new QueryLibraryEntry("id", "Name", "select 1", null, null, false, null, null, now, now);

        assertEquals("", entry.description());
        assertEquals("", entry.environmentScope());
        assertEquals("", entry.connectionScope());
        assertTrue(entry.tags().isEmpty());
    }
}
