package bo.ahosoft.sqlscript.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import bo.ahosoft.sqlscript.domain.QueryLibraryEntry;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class QueryLibraryStoreTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-09T10:00:00Z"), ZoneOffset.UTC);

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void savesAndLoadsSqlWithSlugMetadataAndBase64Persistence() throws Exception {
        File file = new File(temporaryFolder.newFolder("library"), "query-library.properties");
        QueryLibraryStore store = new QueryLibraryStore(file, CLOCK);

        QueryLibraryEntry saved = store.save(
            "Daily Support Report",
            "select * from tickets",
            "Open tickets",
            Arrays.asList("support", "daily"),
            true,
            "PROD",
            "prod-support",
            false
        );

        String stored = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        QueryLibraryEntry loaded = new QueryLibraryStore(file, CLOCK).load("daily-support-report");

        assertEquals("daily-support-report", saved.id());
        assertEquals("Daily Support Report", loaded.name());
        assertEquals("select * from tickets", loaded.sql());
        assertEquals(Arrays.asList("support", "daily"), loaded.tags());
        assertTrue(loaded.favorite());
        assertEquals("PROD", loaded.environmentScope());
        assertEquals("prod-support", loaded.connectionScope());
        assertFalse(stored.contains("select * from tickets"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsDuplicateSlugUnlessOverwriteIsExplicit() throws Exception {
        QueryLibraryStore store = new QueryLibraryStore(temporaryFolder.newFile("duplicates.properties"), CLOCK);

        store.save("Monthly Billing", "select 1", "", Arrays.asList("billing"), false, "", "", false);
        store.save("monthly billing", "select 2", "", Arrays.asList("billing"), false, "", "", false);
    }

    @Test
    public void overwritesExistingEntryWhileKeepingOriginalCreationTime() throws Exception {
        QueryLibraryStore store = new QueryLibraryStore(temporaryFolder.newFile("overwrite.properties"), CLOCK);
        QueryLibraryEntry first = store.save("Incident Review", "select 1", "old", Arrays.asList("incident"), false, "", "", false);

        QueryLibraryEntry overwritten = store.save(
            "Incident Review",
            "select 2",
            "new",
            Arrays.asList("review"),
            true,
            "QA",
            "qa-support",
            true
        );

        assertEquals(first.createdAt(), overwritten.createdAt());
        assertEquals("select 2", store.load("incident-review").sql());
        assertEquals(Arrays.asList("review"), store.load("incident-review").tags());
        assertTrue(store.load("incident-review").favorite());
    }

    @Test
    public void searchesOnlyNameTagsAndDescriptionNotSqlContent() throws Exception {
        QueryLibraryStore store = new QueryLibraryStore(temporaryFolder.newFile("search.properties"), CLOCK);
        store.save("Customer Lookup", "select * from secret_card_table", "Find customer", Arrays.asList("support"), false, "", "", false);
        store.save("Billing Summary", "select * from invoices", "Monthly totals", Arrays.asList("finance"), false, "", "", false);

        assertEquals("customer-lookup", store.search("support").get(0).id());
        assertEquals("billing-summary", store.search("monthly").get(0).id());
        assertTrue(store.search("secret_card_table").isEmpty());
    }

    @Test
    public void deletesAndFavoritesExistingEntriesById() throws Exception {
        QueryLibraryStore store = new QueryLibraryStore(temporaryFolder.newFile("delete.properties"), CLOCK);
        store.save("Keep Query", "select 1", "", Arrays.asList("keep"), false, "", "", false);
        store.save("Drop Query", "select 2", "", Arrays.asList("drop"), false, "", "", false);

        QueryLibraryEntry favorite = store.setFavorite("keep-query", true);
        boolean deleted = store.delete("drop-query");

        assertTrue(favorite.favorite());
        assertTrue(deleted);
        assertEquals(1, store.list().size());
        assertEquals("keep-query", store.list().get(0).id());
    }

    @Test
    public void ignoresUnknownAndCorruptEntriesWhenLoading() throws Exception {
        File file = temporaryFolder.newFile("legacy.properties");
        Properties properties = new Properties();
        properties.setProperty("entries", "valid,corrupt");
        properties.setProperty("entry.valid.name", QueryLibraryStore.encodeText("Valid Query"));
        properties.setProperty("entry.valid.sql", QueryLibraryStore.encodeText("select 1"));
        properties.setProperty("entry.valid.description", QueryLibraryStore.encodeText("Works"));
        properties.setProperty("entry.valid.tags.count", "1");
        properties.setProperty("entry.valid.tags.0", QueryLibraryStore.encodeText("legacy"));
        properties.setProperty("entry.valid.favorite", "true");
        properties.setProperty("entry.valid.createdAt", "2026-06-09T09:00:00Z");
        properties.setProperty("entry.valid.updatedAt", "2026-06-09T09:05:00Z");
        properties.setProperty("entry.valid.future", "ignored");
        properties.setProperty("entry.corrupt.name", "not-base64");
        properties.store(Files.newOutputStream(file.toPath()), "legacy");

        List<QueryLibraryEntry> entries = new QueryLibraryStore(file, CLOCK).list();

        assertEquals(1, entries.size());
        assertEquals("valid", entries.get(0).id());
        assertEquals(Arrays.asList("legacy"), entries.get(0).tags());
    }

    @Test
    public void appliesUserOnlyPermissionsWhenPosixIsSupported() throws Exception {
        File directory = temporaryFolder.newFolder("secure-library");
        File file = new File(directory, "query-library.properties");
        QueryLibraryStore store = new QueryLibraryStore(file, CLOCK);

        store.save("Secure Query", "select 1", "", Arrays.asList("secure"), false, "", "", false);

        assertTrue(QueryLibraryStore.PRIVACY_WARNING.contains("sensitive"));
        try {
            Set<PosixFilePermission> directoryPermissions = Files.getPosixFilePermissions(directory.toPath());
            Set<PosixFilePermission> filePermissions = Files.getPosixFilePermissions(file.toPath());
            assertEquals(QueryLibraryStore.USER_ONLY_DIRECTORY_PERMISSIONS, directoryPermissions);
            assertEquals(QueryLibraryStore.USER_ONLY_FILE_PERMISSIONS, filePermissions);
        } catch (UnsupportedOperationException ignored) {
            assertTrue(file.isFile());
        }
    }
}
