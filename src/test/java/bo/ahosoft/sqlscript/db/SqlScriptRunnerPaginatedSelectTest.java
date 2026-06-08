package bo.ahosoft.sqlscript.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class SqlScriptRunnerPaginatedSelectTest {

    @Test
    public void postgresqlSelectUsesCountAndLimitOffsetWithoutFullFetch() throws Exception {
        RecordingFactory factory = new RecordingFactory(250);
        SqlScriptRunner runner = new SqlScriptRunner(factory);

        SqlExecutionResult result = runner.executeSingle(postgresConfig(), "select id from huge_table;");

        assertEquals(2, factory.sql.size());
        assertEquals("SELECT COUNT(*) FROM (select id from huge_table) q", factory.sql.get(0));
        assertEquals("SELECT * FROM (select id from huge_table) q LIMIT 100 OFFSET 0", factory.sql.get(1));
        assertTrue(result.consoleTable().contains("Page 1/3"));
        assertTrue(result.consoleTable().contains("Rows 1-100"));
        assertTrue(result.consoleTable().contains("100"));
        assertFalse(result.consoleTable().contains("101"));
    }

    @Test
    public void countQueryCalculatesTotalPages() throws Exception {
        RecordingFactory factory = new RecordingFactory(201);

        SqlExecutionResult result = new SqlScriptRunner(factory).executeSingle(postgresConfig(), "select id from huge_table");

        assertEquals(3, result.pageCount());
        assertTrue(result.consoleTable().contains("Page 1/3"));
    }

    @Test
    public void pageDownFetchesOnlyNextPageSelect() throws Exception {
        RecordingFactory factory = new RecordingFactory(250);
        SqlExecutionResult first = new SqlScriptRunner(factory).executeSingle(postgresConfig(), "select id from huge_table");

        SqlExecutionResult second = first.nextPage();
        String secondPage = second.consoleTable();

        assertEquals(3, factory.sql.size());
        assertEquals("SELECT * FROM (select id from huge_table) q LIMIT 100 OFFSET 100", factory.sql.get(2));
        assertTrue(secondPage.contains("Page 2/3"));
        assertTrue(secondPage.contains("Rows 101-200"));
        assertFalse(secondPage.contains("201"));
    }

    @Test
    public void cachedPageIsReusedWhenNavigatingBackBeforeTtlExpiry() throws Exception {
        RecordingFactory factory = new RecordingFactory(250);
        SqlExecutionResult first = new SqlScriptRunner(factory).executeSingle(postgresConfig(), "select id from huge_table");
        SqlExecutionResult second = first.nextPage();
        second.consoleTable();

        SqlExecutionResult backToFirst = second.previousPage();

        assertTrue(backToFirst.consoleTable().contains("Page 1/3"));
        assertEquals(3, factory.sql.size());
    }

    @Test
    public void rerunningSameQueryCreatesFreshCacheAndQueriesAgain() throws Exception {
        RecordingFactory factory = new RecordingFactory(250);
        SqlScriptRunner runner = new SqlScriptRunner(factory);

        runner.executeSingle(postgresConfig(), "select id from huge_table");
        runner.executeSingle(postgresConfig(), "select id from huge_table");

        assertEquals(
            Arrays.asList(
                "SELECT COUNT(*) FROM (select id from huge_table) q",
                "SELECT * FROM (select id from huge_table) q LIMIT 100 OFFSET 0",
                "SELECT COUNT(*) FROM (select id from huge_table) q",
                "SELECT * FROM (select id from huge_table) q LIMIT 100 OFFSET 0"
            ),
            factory.sql
        );
    }

    @Test
    public void ttlExpiryForcesPageRequery() throws Exception {
        RecordingFactory factory = new RecordingFactory(250);
        MutableClock clock = new MutableClock();
        SqlScriptRunner runner = new SqlScriptRunner(factory, clock);
        SqlExecutionResult first = runner.executeSingle(postgresConfig(), "select id from huge_table");

        clock.now = clock.now + SqlExecutionResult.PAGE_CACHE_TTL_MILLIS + 1;
        first.consoleTable();

        assertEquals(3, factory.sql.size());
        assertEquals("SELECT * FROM (select id from huge_table) q LIMIT 100 OFFSET 0", factory.sql.get(2));
    }

    @Test
    public void oracleSelectUsesOffsetFetchPagination() throws Exception {
        RecordingFactory factory = new RecordingFactory(150);

        SqlExecutionResult result = new SqlScriptRunner(factory).executeSingle(oracleConfig(), "select id from huge_table");

        assertEquals(2, factory.sql.size());
        assertEquals("SELECT COUNT(*) FROM (select id from huge_table) q", factory.sql.get(0));
        assertEquals("SELECT * FROM (select id from huge_table) q OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY", factory.sql.get(1));
        assertTrue(result.consoleTable().contains("Page 1/2"));
        assertTrue(result.consoleTable().contains("Rows 1-100"));
        assertFalse(result.consoleTable().contains("full-result-should-not-be-read"));
    }

    @Test
    public void oraclePageDownFetchesOnlyNextOffsetFetchPage() throws Exception {
        RecordingFactory factory = new RecordingFactory(250);
        SqlExecutionResult first = new SqlScriptRunner(factory).executeSingle(oracleConfig(), "select id from huge_table");

        SqlExecutionResult second = first.nextPage();
        String secondPage = second.consoleTable();

        assertEquals(3, factory.sql.size());
        assertEquals("SELECT * FROM (select id from huge_table) q OFFSET 100 ROWS FETCH NEXT 100 ROWS ONLY", factory.sql.get(2));
        assertTrue(secondPage.contains("Page 2/3"));
        assertTrue(secondPage.contains("Rows 101-200"));
        assertFalse(secondPage.contains("full-result-should-not-be-read"));
    }

    @Test
    public void oracleCachedPageIsReusedAndExpiresAfterTtl() throws Exception {
        RecordingFactory factory = new RecordingFactory(250);
        MutableClock clock = new MutableClock();
        SqlExecutionResult first = new SqlScriptRunner(factory, clock).executeSingle(oracleConfig(), "select id from huge_table");
        SqlExecutionResult second = first.nextPage();
        second.consoleTable();

        SqlExecutionResult backToFirst = second.previousPage();

        assertTrue(backToFirst.consoleTable().contains("Page 1/3"));
        assertEquals(3, factory.sql.size());

        clock.now = clock.now + SqlExecutionResult.PAGE_CACHE_TTL_MILLIS + 1;
        backToFirst.consoleTable();

        assertEquals(4, factory.sql.size());
        assertEquals("SELECT * FROM (select id from huge_table) q OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY", factory.sql.get(3));
    }

    @Test
    public void oracleRerunningSameQueryCreatesFreshCacheAndQueriesAgain() throws Exception {
        RecordingFactory factory = new RecordingFactory(250);
        SqlScriptRunner runner = new SqlScriptRunner(factory);

        runner.executeSingle(oracleConfig(), "select id from huge_table");
        runner.executeSingle(oracleConfig(), "select id from huge_table");

        assertEquals(
            Arrays.asList(
                "SELECT COUNT(*) FROM (select id from huge_table) q",
                "SELECT * FROM (select id from huge_table) q OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY",
                "SELECT COUNT(*) FROM (select id from huge_table) q",
                "SELECT * FROM (select id from huge_table) q OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY"
            ),
            factory.sql
        );
    }

    @Test
    public void countFailureStillShowsCurrentPageWithUnknownTotal() throws Exception {
        RecordingFactory factory = new RecordingFactory(250);
        factory.failCount = true;

        SqlExecutionResult result = new SqlScriptRunner(factory).executeSingle(postgresConfig(), "select id from huge_table");

        assertEquals(2, factory.sql.size());
        assertTrue(result.consoleTable().contains("Page 1/?"));
        assertTrue(result.consoleTable().contains("Rows 1-100"));
    }

    private static ConnectionConfig postgresConfig() {
        return new ConnectionConfig(DatabaseType.POSTGRESQL, "jdbc:postgresql://localhost/app", "app", "secret", Arrays.asList("public"));
    }

    private static ConnectionConfig oracleConfig() {
        return new ConnectionConfig(
            DatabaseType.ORACLE,
            "jdbc:oracle:thin:@localhost:1521/XEPDB1",
            "app",
            "secret",
            Arrays.<String>asList()
        );
    }

    private static final class MutableClock implements SqlScriptRunner.Clock {

        private long now = 1000L;

        @Override
        public long currentTimeMillis() {
            return now;
        }
    }

    private static final class RecordingFactory extends JdbcConnectionFactory {

        private final List<String> sql = new ArrayList<String>();
        private final int totalRows;
        private boolean failCount;

        RecordingFactory(int totalRows) {
            this.totalRows = totalRows;
        }

        @Override
        public Connection open(ConnectionConfig config) throws SQLException {
            return ProxyJdbc.connection(
                new ProxyJdbc.StatementHandler() {
                    @Override
                    public ResultSet executeQuery(String statement) throws SQLException {
                        sql.add(statement);
                        if (statement.startsWith("SELECT COUNT(*)")) {
                            if (failCount) {
                                throw new SQLException("count failed");
                            }
                            return ProxyJdbc.resultSet("count", String.valueOf(totalRows));
                        }
                        return pageResult(statement);
                    }

                    @Override
                    public boolean execute(String statement) {
                        sql.add(statement);
                        return true;
                    }

                    @Override
                    public ResultSet resultSet() {
                        return ProxyJdbc.resultSet("id", "full-result-should-not-be-read");
                    }
                }
            );
        }

        private ResultSet pageResult(String statement) {
            int offset = offset(statement);
            List<String> values = new ArrayList<String>();
            for (int value = offset + 1; value <= Math.min(offset + 100, totalRows); value++) {
                values.add(String.valueOf(value));
            }
            return ProxyJdbc.resultSet("id", values.toArray(new String[values.size()]));
        }

        private static int offset(String statement) {
            int limitOffset = statement.indexOf(" OFFSET ");
            if (limitOffset >= 0) {
                String value = statement.substring(limitOffset + " OFFSET ".length()).split(" ")[0];
                return Integer.parseInt(value);
            }
            int oracleOffset = statement.indexOf(" OFFSET ");
            if (oracleOffset >= 0) {
                String value = statement.substring(oracleOffset + " OFFSET ".length()).split(" ")[0];
                return Integer.parseInt(value);
            }
            return 0;
        }
    }
}
