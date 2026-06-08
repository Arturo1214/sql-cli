package bo.ahosoft.sqlscript.config;

import static org.junit.Assert.assertEquals;
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
import java.util.Properties;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ConfigStoreTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void savesAndLoadsTypeAndCommaSeparatedSchemas() throws Exception {
        File file = temporaryFolder.newFile("postgres.properties");
        ConnectionConfig config = new ConnectionConfig(
            DatabaseType.POSTGRESQL,
            "jdbc:postgresql://localhost:5432/app",
            "pg_user",
            "pg-secret",
            Arrays.asList("app", "audit")
        );

        ConfigStore.save(file, config);

        String stored = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        assertTrue(stored.contains("type=POSTGRESQL"));
        assertTrue(stored.contains("schemas=app,audit"));

        ConnectionConfig loaded = ConfigStore.load(file);
        assertEquals(DatabaseType.POSTGRESQL, loaded.databaseType());
        assertEquals(Arrays.asList("app", "audit"), loaded.schemas());
    }

    @Test
    public void loadsLegacyProfilesAsOracleWithNoSchemas() throws Exception {
        File file = temporaryFolder.newFile("legacy.properties");
        Properties properties = new Properties();
        properties.setProperty("jdbcUrl", "jdbc:oracle:thin:@legacy:1521/QA");
        properties.setProperty("username", "legacy_user");
        properties.setProperty("password", "legacy-secret");
        properties.store(Files.newOutputStream(file.toPath()), "legacy");

        ConnectionConfig loaded = ConfigStore.load(file);

        assertEquals(DatabaseType.ORACLE, loaded.databaseType());
        assertTrue(loaded.schemas().isEmpty());
    }

    @Test
    public void ignoresSchemaFieldsForOracleProfiles() throws Exception {
        File file = temporaryFolder.newFile("oracle-with-schema.properties");
        Properties properties = new Properties();
        properties.setProperty("type", "ORACLE");
        properties.setProperty("jdbcUrl", "jdbc:oracle:thin:@localhost:1521/XEPDB1");
        properties.setProperty("username", "oracle_user");
        properties.setProperty("password", "oracle-secret");
        properties.setProperty("schemas", "app,audit");
        properties.store(Files.newOutputStream(file.toPath()), "oracle");

        ConnectionConfig loaded = ConfigStore.load(file);

        assertEquals(DatabaseType.ORACLE, loaded.databaseType());
        assertTrue(loaded.schemas().isEmpty());
    }
}
