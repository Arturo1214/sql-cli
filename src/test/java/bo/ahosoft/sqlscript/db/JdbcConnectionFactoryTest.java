package bo.ahosoft.sqlscript.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;
import java.util.Arrays;
import java.util.Properties;
import org.junit.Test;

public class JdbcConnectionFactoryTest {

    @Test
    public void createsPostgresqlPropertiesWithCurrentSchema() {
        ConnectionConfig config = new ConnectionConfig(
            DatabaseType.POSTGRESQL,
            "jdbc:postgresql://localhost/app",
            "app",
            "secret",
            Arrays.asList("public", "audit")
        );

        Properties properties = JdbcConnectionFactory.connectionProperties(config);

        assertEquals("app", properties.getProperty("user"));
        assertEquals("secret", properties.getProperty("password"));
        assertEquals("public,audit", properties.getProperty("currentSchema"));
    }

    @Test
    public void omitsCurrentSchemaForOracleConnections() {
        ConnectionConfig config = new ConnectionConfig("jdbc:oracle:thin:@//localhost:1521/XEPDB1", "app", "secret");

        Properties properties = JdbcConnectionFactory.connectionProperties(config);

        assertEquals("app", properties.getProperty("user"));
        assertFalse(properties.containsKey("currentSchema"));
    }
}
