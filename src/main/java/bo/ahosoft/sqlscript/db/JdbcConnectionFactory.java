package bo.ahosoft.sqlscript.db;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

public class JdbcConnectionFactory {

    private static final String TEST_DRIVER_PROPERTY = "oracleScriptCli.testDriver";

    public Connection open(ConnectionConfig config) throws SQLException {
        loadConfiguredDriver();
        return DriverManager.getConnection(config.jdbcUrl(), connectionProperties(config));
    }

    private static void loadConfiguredDriver() throws SQLException {
        String driverClassName = System.getProperty(TEST_DRIVER_PROPERTY);
        if (driverClassName == null || driverClassName.trim().isEmpty()) {
            return;
        }
        try {
            Class.forName(driverClassName.trim());
        } catch (ClassNotFoundException ex) {
            SQLException sqlException = new SQLException("Configured JDBC driver not found: " + driverClassName);
            sqlException.initCause(ex);
            throw sqlException;
        }
    }

    public static Properties connectionProperties(ConnectionConfig config) {
        Properties properties = new Properties();
        properties.setProperty("user", config.username());
        properties.setProperty("password", config.password());
        if (config.databaseType() == DatabaseType.POSTGRESQL && !config.schemas().isEmpty()) {
            properties.setProperty("currentSchema", join(config.schemas()));
        }
        return properties;
    }

    private static String join(List<String> values) {
        StringBuilder joined = new StringBuilder();
        for (String value : values) {
            if (joined.length() > 0) {
                joined.append(',');
            }
            joined.append(value);
        }
        return joined.toString();
    }
}
