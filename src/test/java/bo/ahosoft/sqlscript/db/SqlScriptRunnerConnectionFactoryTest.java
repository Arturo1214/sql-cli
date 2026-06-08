package bo.ahosoft.sqlscript.db;

import static org.junit.Assert.assertEquals;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class SqlScriptRunnerConnectionFactoryTest {

    @Test
    public void opensConnectionsThroughFactoryBeforeExecutingSql() throws Exception {
        RecordingFactory factory = new RecordingFactory();
        ConnectionConfig config = new ConnectionConfig(
            DatabaseType.POSTGRESQL,
            "jdbc:postgresql://localhost/app",
            "app",
            "secret",
            java.util.Arrays.asList("public")
        );

        SqlExecutionResult result = new SqlScriptRunner(factory).execute(config, "select id from users");

        assertEquals(config, factory.openedConfig);
        org.junit.Assert.assertTrue(result.consoleTable().contains("id"));
        org.junit.Assert.assertTrue(result.consoleTable().contains("1"));
    }

    @Test
    public void selectResultsAreFetchedAsTransparentDatabasePagesOfOneHundredRows() throws Exception {
        RecordingFactory factory = new RecordingFactory(oneToOneHundredFive());
        ConnectionConfig config = new ConnectionConfig(
            DatabaseType.POSTGRESQL,
            "jdbc:postgresql://localhost/app",
            "app",
            "secret",
            java.util.Arrays.asList("public")
        );

        SqlExecutionResult result = new SqlScriptRunner(factory).executeSingle(config, "select id from users");

        assertEquals(100, result.pageSize());
        assertEquals(2, result.pageCount());
        assertEquals(0, result.pageIndex());
        org.junit.Assert.assertTrue(result.consoleTable().contains("Rows 1-100"));
        org.junit.Assert.assertTrue(result.consoleTable().contains("100"));
        org.junit.Assert.assertFalse(result.consoleTable().contains("101"));
        org.junit.Assert.assertTrue(result.nextPage().consoleTable().contains("Rows 101-105"));
        org.junit.Assert.assertTrue(result.nextPage().consoleTable().contains("105"));
        org.junit.Assert.assertEquals("SELECT COUNT(*) FROM (select id from users) q", factory.sql.get(0));
        org.junit.Assert.assertEquals("SELECT * FROM (select id from users) q LIMIT 100 OFFSET 0", factory.sql.get(1));
        org.junit.Assert.assertEquals("SELECT * FROM (select id from users) q LIMIT 100 OFFSET 100", factory.sql.get(2));
    }

    private static String[] oneToOneHundredFive() {
        List<String> values = new ArrayList<String>();
        for (int i = 1; i <= 105; i++) {
            values.add(String.valueOf(i));
        }
        return values.toArray(new String[values.size()]);
    }

    private static final class RecordingFactory extends JdbcConnectionFactory {

        private ConnectionConfig openedConfig;
        private final String[] values;
        private final List<String> sql = new ArrayList<String>();

        RecordingFactory() {
            this(new String[] { "1" });
        }

        RecordingFactory(String[] values) {
            this.values = values;
        }

        @Override
        public Connection open(ConnectionConfig config) throws SQLException {
            openedConfig = config;
            return ProxyJdbc.connection(
                new ProxyJdbc.StatementHandler() {
                    @Override
                    public ResultSet executeQuery(String sql) throws SQLException {
                        RecordingFactory.this.sql.add(sql);
                        if (sql.startsWith("SELECT COUNT(*)")) {
                            return ProxyJdbc.resultSet("count", String.valueOf(values.length));
                        }
                        int offset = sql.endsWith("OFFSET 100") ? 100 : 0;
                        List<String> page = new ArrayList<String>();
                        for (int i = offset; i < Math.min(offset + 100, values.length); i++) {
                            page.add(values[i]);
                        }
                        return ProxyJdbc.resultSet("id", page.toArray(new String[page.size()]));
                    }

                    @Override
                    public boolean execute(String sql) {
                        RecordingFactory.this.sql.add(sql);
                        return true;
                    }

                    @Override
                    public ResultSet resultSet() {
                        return ProxyJdbc.resultSet("id", values);
                    }
                }
            );
        }
    }
}
