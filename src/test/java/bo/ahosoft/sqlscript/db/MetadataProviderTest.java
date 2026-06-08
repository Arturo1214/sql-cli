package bo.ahosoft.sqlscript.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;
import java.util.Arrays;
import org.junit.Test;

public class MetadataProviderTest {

    @Test
    public void createsOracleProviderForOracleConnections() {
        MetadataProvider provider = MetadataProviderFactory.forConfig(
            new ConnectionConfig("jdbc:oracle:thin:@//localhost:1521/XEPDB1", "app", "secret")
        );

        assertEquals("select table_name from user_tables order by table_name", provider.tables(null));
        assertTrue(provider.describe("users").contains("from user_tab_columns"));
        assertEquals("select * from USERS where rownum <= 10", provider.sample("USERS", 10));
    }

    @Test
    public void createsPostgresProviderForPostgresqlConnections() {
        ConnectionConfig config = new ConnectionConfig(
            DatabaseType.POSTGRESQL,
            "jdbc:postgresql://localhost/app",
            "app",
            "secret",
            Arrays.asList("public", "audit")
        );

        MetadataProvider provider = MetadataProviderFactory.forConfig(config);

        assertTrue(provider.tables("user").contains("information_schema.tables"));
        assertTrue(provider.tables("user").contains("table_schema in ('public','audit')"));
        assertTrue(provider.describe("users").contains("information_schema.columns"));
        assertEquals("select * from users limit 5", provider.sample("users", 5));
    }

    @Test
    public void escapesMetadataLiteralsAndRejectsUnsafeIdentifiers() {
        MetadataProvider provider = new PostgresMetadataProvider(Arrays.asList("public"));

        assertTrue(provider.search("owner's").contains("owner''s"));
        try {
            provider.sample("users; drop table users", 5);
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage().contains("Invalid identifier"));
            return;
        }
        throw new AssertionError("Expected unsafe table name to be rejected");
    }

    @Test
    public void exposesAllMetadataCommandsForBothProviders() {
        MetadataProvider oracle = new OracleMetadataProvider();
        MetadataProvider postgres = new PostgresMetadataProvider(Arrays.asList("public"));

        assertTrue(oracle.details("USERS").contains("user_tab_columns"));
        assertTrue(oracle.indexes("USERS").contains("user_indexes"));
        assertTrue(oracle.constraints("USERS").contains("user_constraints"));
        assertTrue(oracle.fkIn("USERS").contains("refc.table_name"));
        assertTrue(oracle.fkOut("USERS").contains("fk.table_name"));
        assertEquals("select count(*) as TOTAL from USERS", oracle.count("USERS"));
        assertTrue(oracle.explain("select * from users;").contains("dbms_xplan.display()"));

        assertTrue(postgres.details("users").contains("information_schema.columns"));
        assertTrue(postgres.indexes("users").contains("pg_indexes"));
        assertTrue(postgres.constraints("users").contains("information_schema.table_constraints"));
        assertTrue(postgres.fkIn("users").contains("foreign_table_name"));
        assertTrue(postgres.fkOut("users").contains("references_table"));
        assertEquals("select count(*) as total from users", postgres.count("users"));
        assertEquals("explain select * from users", postgres.explain("select * from users;"));
    }
}
