package bo.ahosoft.sqlscript.db;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;

public final class StubPostgresDriver implements Driver {

    public static final String URL = "jdbc:postgresql://oracle-script-cli-stub/app";

    static {
        try {
            DriverManager.registerDriver(new StubPostgresDriver());
        } catch (SQLException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        if (!acceptsURL(url)) {
            return null;
        }
        return ProxyJdbc.connection(
            new ProxyJdbc.StatementHandler() {
                @Override
                public ResultSet executeQuery(String sql) {
                    return ProxyJdbc.resultSet("schema_name", "app", "public", "reporting");
                }

                @Override
                public boolean execute(String sql) {
                    return true;
                }

                @Override
                public ResultSet resultSet() {
                    return ProxyJdbc.resultSet("schema_name", "app", "public", "reporting");
                }
            }
        );
    }

    @Override
    public boolean acceptsURL(String url) {
        return URL.equals(url);
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) {
        return new DriverPropertyInfo[0];
    }

    @Override
    public int getMajorVersion() {
        return 1;
    }

    @Override
    public int getMinorVersion() {
        return 0;
    }

    @Override
    public boolean jdbcCompliant() {
        return false;
    }

    @Override
    public Logger getParentLogger() {
        return Logger.getGlobal();
    }
}
