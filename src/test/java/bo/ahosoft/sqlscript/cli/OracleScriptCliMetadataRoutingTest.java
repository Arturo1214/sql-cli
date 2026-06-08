package bo.ahosoft.sqlscript.cli;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

public class OracleScriptCliMetadataRoutingTest {

    @Test
    public void tablesUsesPostgreSqlMetadataForPostgreSqlConnections() {
        ConnectionConfig config = new ConnectionConfig(
            DatabaseType.POSTGRESQL,
            "jdbc:postgresql://localhost:5432/app",
            "pg_user",
            "pg-secret",
            Arrays.asList("app")
        );

        String sql = OracleScriptCli.metadataSql(config, "tables", Collections.<String>emptyList());

        assertTrue(sql.contains("information_schema.tables"));
        assertTrue(sql.contains("table_schema in ('app')"));
        assertFalse(sql.contains("user_tables"));
    }

    @Test
    public void describeUsesOracleMetadataForOracleConnections() {
        ConnectionConfig config = new ConnectionConfig(
            DatabaseType.ORACLE,
            "jdbc:oracle:thin:@localhost:1521/XEPDB1",
            "ora_user",
            "ora-secret",
            Collections.<String>emptyList()
        );

        String sql = OracleScriptCli.metadataSql(config, "describe", Arrays.asList("CUSTOMERS"));

        assertTrue(sql.contains("user_tab_columns"));
        assertTrue(sql.contains("table_name = upper('CUSTOMERS')"));
        assertFalse(sql.contains("information_schema.columns"));
    }

    @Test
    public void sampleUsesPostgreSqlLimitInsteadOfOracleRownum() {
        ConnectionConfig config = new ConnectionConfig(
            DatabaseType.POSTGRESQL,
            "jdbc:postgresql://localhost:5432/app",
            "pg_user",
            "pg-secret",
            Collections.<String>emptyList()
        );

        String sql = OracleScriptCli.metadataSql(config, "sample", Arrays.asList("customers", "5"));

        assertTrue(sql.contains("select * from customers limit 5"));
        assertFalse(sql.contains("rownum"));
    }

    @Test
    public void sampleUsesOracleRownumForOracleConnections() {
        ConnectionConfig config = new ConnectionConfig(
            DatabaseType.ORACLE,
            "jdbc:oracle:thin:@localhost:1521/XEPDB1",
            "ora_user",
            "ora-secret",
            Collections.<String>emptyList()
        );

        String sql = OracleScriptCli.metadataSql(config, "sample", Arrays.asList("CUSTOMERS", "5"));

        assertTrue(sql.contains("select * from CUSTOMERS where rownum <= 5"));
        assertFalse(sql.contains(" limit 5"));
    }
}
