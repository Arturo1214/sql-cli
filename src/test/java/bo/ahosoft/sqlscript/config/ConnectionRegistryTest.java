package bo.ahosoft.sqlscript.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ConnectionRegistryTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void savesAndLoadsNamedConnectionWithProtectedPassword() throws Exception {
        ConnectionRegistry registry = registry();
        ConnectionConfig config = new ConnectionConfig("jdbc:oracle:thin:@localhost:1521/XEPDB1", "qa_user", "qa-secret");

        registry.save("qa", config);

        ConnectionConfig loaded = registry.load("qa");
        assertEquals(config.jdbcUrl(), loaded.jdbcUrl());
        assertEquals(config.username(), loaded.username());
        assertEquals(config.password(), loaded.password());
        assertEquals(ConnectionEnvironment.DEV, loaded.environment());

        String stored = readConnectionFile("qa");
        assertTrue(stored.contains("username=qa_user"));
        assertFalse(stored.contains("qa-secret"));
    }

    @Test
    public void rejectsBlankRequiredFields() throws Exception {
        ConnectionRegistry registry = registry();

        assertRejected(registry, " ", new ConnectionConfig("jdbc:oracle:thin:@localhost:1521/XEPDB1", "qa", "secret"));
        assertRejected(registry, "qa", new ConnectionConfig(" ", "qa", "secret"));
        assertRejected(registry, "qa", new ConnectionConfig("jdbc:oracle:thin:@localhost:1521/XEPDB1", " ", "secret"));
        assertRejected(registry, "qa", new ConnectionConfig("jdbc:oracle:thin:@localhost:1521/XEPDB1", "qa", " "));
    }

    @Test
    public void listsConnectionsWithoutPasswordExposure() throws Exception {
        ConnectionRegistry registry = registry();
        registry.save("qa", new ConnectionConfig("jdbc:oracle:thin:@localhost:1521/XEPDB1", "qa_user", "qa-secret"));

        List<ConnectionRegistry.ConnectionSummary> summaries = registry.list();

        assertEquals(1, summaries.size());
        assertEquals("qa", summaries.get(0).name());
        assertEquals("jdbc:oracle:thin:@localhost:1521/XEPDB1", summaries.get(0).jdbcUrl());
        assertEquals("qa_user", summaries.get(0).username());
        assertFalse(summaries.toString().contains("qa-secret"));
    }

    @Test
    public void importsLegacyPlainTextConfigIntoProtectedRegistry() throws Exception {
        ConnectionRegistry registry = registry();
        File legacy = temporaryFolder.newFile("legacy.properties");
        ConfigStore.save(legacy, new ConnectionConfig("jdbc:oracle:thin:@legacy:1521/QA", "legacy_user", "legacy-secret"));

        ConfigStore.importLegacyProfile("legacy", legacy, registry);

        ConnectionConfig loaded = registry.load("legacy");
        assertEquals("jdbc:oracle:thin:@legacy:1521/QA", loaded.jdbcUrl());
        assertEquals("legacy_user", loaded.username());
        assertEquals("legacy-secret", loaded.password());
        assertFalse(readConnectionFile("legacy").contains("legacy-secret"));
    }

    @Test
    public void savesAndLoadsPostgreSqlTypeAndSchemasWithProtectedPassword() throws Exception {
        ConnectionRegistry registry = registry();
        ConnectionConfig config = new ConnectionConfig(
            DatabaseType.POSTGRESQL,
            "jdbc:postgresql://localhost:5432/app",
            "pg_user",
            "pg-secret",
            Arrays.asList("app", "audit")
        );

        registry.save("pg", config);

        ConnectionConfig loaded = registry.load("pg");
        assertEquals(DatabaseType.POSTGRESQL, loaded.databaseType());
        assertEquals(ConnectionEnvironment.DEV, loaded.environment());
        assertEquals(Arrays.asList("app", "audit"), loaded.schemas());
        assertEquals("pg-secret", loaded.password());

        String stored = readConnectionFile("pg");
        assertTrue(stored.contains("type=POSTGRESQL"));
        assertTrue(stored.contains("schemas=app,audit"));
        assertFalse(stored.contains("pg-secret"));
    }

    @Test
    public void rejectsJdbcUrlThatDoesNotMatchDatabaseType() throws Exception {
        ConnectionRegistry registry = registry();

        assertRejected(
            registry,
            "pg",
            new ConnectionConfig(
                DatabaseType.POSTGRESQL,
                "jdbc:oracle:thin:@localhost:1521/XEPDB1",
                "pg_user",
                "secret",
                Arrays.asList("public")
            )
        );
    }

    @Test
    public void summariesIncludeDatabaseTypeAndSchemasWithoutPassword() throws Exception {
        ConnectionRegistry registry = registry();
        registry.save(
            "pg",
            new ConnectionConfig(
                DatabaseType.POSTGRESQL,
                "jdbc:postgresql://localhost:5432/app",
                "pg_user",
                "pg-secret",
                Arrays.asList("app", "audit")
            )
        );

        List<ConnectionRegistry.ConnectionSummary> summaries = registry.list();

        assertEquals(DatabaseType.POSTGRESQL, summaries.get(0).databaseType());
        assertEquals(ConnectionEnvironment.DEV, summaries.get(0).environment());
        assertEquals(Arrays.asList("app", "audit"), summaries.get(0).schemas());
        assertTrue(summaries.get(0).toString().contains("POSTGRESQL"));
        assertTrue(summaries.get(0).toString().contains("app,audit"));
        assertFalse(summaries.get(0).toString().contains("pg-secret"));
    }

    @Test
    public void savesListsAndLoadsConnectionEnvironment() throws Exception {
        ConnectionRegistry registry = registry();
        ConnectionConfig config = new ConnectionConfig(
            DatabaseType.ORACLE,
            ConnectionEnvironment.PROD,
            "jdbc:oracle:thin:@prod:1521/PROD",
            "support",
            "secret",
            Arrays.<String>asList()
        );

        registry.save("billing-prod", config);

        assertEquals(ConnectionEnvironment.PROD, registry.load("billing-prod").environment());
        assertEquals(ConnectionEnvironment.PROD, registry.list().get(0).environment());
        assertTrue(readConnectionFile("billing-prod").contains("environment=PROD"));
        assertTrue(registry.list().get(0).toString().contains("PROD"));
    }

    @Test
    public void updatesExistingConnectionAndRenamesProfileWithoutExposingPassword() throws Exception {
        ConnectionRegistry registry = registry();
        registry.save("qa", new ConnectionConfig("jdbc:oracle:thin:@localhost:1521/QA", "qa", "old-secret"));

        registry.update(
            "qa",
            "qa-prod",
            new ConnectionConfig(
                DatabaseType.ORACLE,
                ConnectionEnvironment.PROD,
                "jdbc:oracle:thin:@prod:1521/PROD",
                "support",
                "new-secret",
                Arrays.<String>asList()
            )
        );

        assertFalse(registry.exists("qa"));
        assertTrue(registry.exists("qa-prod"));
        ConnectionConfig loaded = registry.load("qa-prod");
        assertEquals(ConnectionEnvironment.PROD, loaded.environment());
        assertEquals("jdbc:oracle:thin:@prod:1521/PROD", loaded.jdbcUrl());
        assertEquals("support", loaded.username());
        assertEquals("new-secret", loaded.password());
        assertFalse(readConnectionFile("qa-prod").contains("new-secret"));
    }

    @Test
    public void updateRejectsMissingSourceAndDuplicateTarget() throws Exception {
        ConnectionRegistry registry = registry();
        registry.save("qa", new ConnectionConfig("jdbc:oracle:thin:@localhost:1521/QA", "qa", "secret"));
        registry.save("prod", new ConnectionConfig("jdbc:oracle:thin:@prod:1521/PROD", "prod", "secret"));

        assertUpdateRejected(
            registry,
            "missing",
            "renamed",
            new ConnectionConfig("jdbc:oracle:thin:@localhost:1521/QA", "qa", "secret"),
            "does not exist"
        );
        assertUpdateRejected(
            registry,
            "qa",
            "prod",
            new ConnectionConfig("jdbc:oracle:thin:@localhost:1521/QA", "qa", "secret"),
            "already exists"
        );
    }

    @Test
    public void deletesExistingConnectionAndReportsMissingProfiles() throws Exception {
        ConnectionRegistry registry = registry();
        registry.save("qa", new ConnectionConfig("jdbc:oracle:thin:@localhost:1521/QA", "qa", "secret"));

        assertTrue(registry.delete("qa"));
        assertFalse(registry.exists("qa"));
        assertFalse(registry.delete("qa"));
        assertTrue(registry.list().isEmpty());
    }

    private ConnectionRegistry registry() throws Exception {
        File baseDirectory = temporaryFolder.newFolder("connections");
        ProtectedSecretStore secretStore = new ProtectedSecretStore(new File(baseDirectory, "secrets"));
        return new ConnectionRegistry(baseDirectory, secretStore);
    }

    private String readConnectionFile(String name) throws Exception {
        File file = new File(new File(temporaryFolder.getRoot(), "connections"), name + ".properties");
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    private static void assertRejected(ConnectionRegistry registry, String name, ConnectionConfig config) throws Exception {
        try {
            registry.save(name, config);
        } catch (IllegalArgumentException ex) {
            if (config.password() != null && !config.password().trim().isEmpty()) {
                assertFalse(ex.getMessage().contains(config.password()));
            }
            return;
        }
        throw new AssertionError("Expected validation rejection");
    }

    private static void assertUpdateRejected(
        ConnectionRegistry registry,
        String oldName,
        String newName,
        ConnectionConfig config,
        String expectedMessage
    ) throws Exception {
        try {
            registry.update(oldName, newName, config);
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage().contains(expectedMessage));
            return;
        }
        throw new AssertionError("Expected update rejection");
    }
}
