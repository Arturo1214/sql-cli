package bo.ahosoft.sqlscript.db;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class SqlScriptRunner {

    private final JdbcConnectionFactory connectionFactory;
    private final Clock clock;

    public SqlScriptRunner() {
        this(new JdbcConnectionFactory());
    }

    public SqlScriptRunner(JdbcConnectionFactory connectionFactory) {
        this(connectionFactory, new SystemClock());
    }

    public SqlScriptRunner(JdbcConnectionFactory connectionFactory, Clock clock) {
        this.connectionFactory = connectionFactory;
        this.clock = clock == null ? new SystemClock() : clock;
    }

    public SqlExecutionResult execute(ConnectionConfig config, String script) throws SQLException {
        return execute(config, script, connectionFactory.open(config));
    }

    public SqlExecutionResult executeSingle(ConnectionConfig config, String statement) throws SQLException {
        return execute(config, requireSingleStatement(statement));
    }

    public static String currentStatement(String buffer, int cursorOffset) {
        return requireSingleStatement(SqlStatementSelector.currentStatement(buffer, cursorOffset));
    }

    public static String requireSingleStatement(String script) {
        List<String> statements = splitStatements(script);
        if (statements.size() != 1) {
            throw new IllegalArgumentException("Expected a single statement, but found " + statements.size());
        }
        return statements.get(0);
    }

    public SqlExecutionResult execute(String jdbcUrl, String username, String password, String script) throws SQLException {
        return execute(new ConnectionConfig(jdbcUrl, username, password), script);
    }

    private SqlExecutionResult execute(ConnectionConfig config, String script, Connection connection) throws SQLException {
        String jdbcUrl = config.jdbcUrl();
        String username = config.username();
        if (isBlank(jdbcUrl)) {
            throw new IllegalArgumentException("jdbcUrl es obligatorio");
        }
        if (isBlank(username)) {
            throw new IllegalArgumentException("username es obligatorio");
        }
        if (script == null || script.trim().isEmpty()) {
            throw new IllegalArgumentException("script es obligatorio");
        }

        StringBuilder output = new StringBuilder();
        SqlExecutionResult singlePagedResult = null;
        try (Connection ignored = connection) {
            List<String> statements = splitStatements(script);
            if (statements.size() == 1 && isSelectLike(statements.get(0))) {
                String sql = statements.get(0);
                return paginatedSelect(config, "SQL #1: " + sql, sql);
            }
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                for (int i = 0; i < statements.size(); i++) {
                    String sql = statements.get(i);
                    String heading = "SQL #" + (i + 1) + ": " + sql;

                    boolean hasResultSet = statement.execute(sql);
                    if (hasResultSet) {
                        try (ResultSet resultSet = statement.getResultSet()) {
                            SqlExecutionResult result = formatResultSet(heading, resultSet);
                            if (statements.size() == 1 && output.length() == 0) {
                                singlePagedResult = result;
                            } else {
                                output.append(result.consoleTable());
                            }
                        }
                    } else {
                        output.append(heading).append(System.lineSeparator());
                        int updateCount = statement.getUpdateCount();
                        output
                            .append(updateCount >= 0 ? "Filas afectadas: " + updateCount : "Sentencia ejecutada")
                            .append(System.lineSeparator());
                    }
                    output.append(System.lineSeparator());
                }
                connection.commit();
            } catch (RuntimeException | SQLException ex) {
                connection.rollback();
                throw ex;
            }
        }
        return singlePagedResult == null ? new SqlExecutionResult(output.toString()) : singlePagedResult;
    }

    private SqlExecutionResult paginatedSelect(ConnectionConfig config, String heading, String sql) throws SQLException {
        String normalizedSql = stripTrailingSemicolon(sql);
        Long totalRows = null;
        try {
            totalRows = countRows(config, normalizedSql);
        } catch (SQLException ignored) {
            totalRows = null;
        }
        return SqlExecutionResult.databasePaged(new JdbcPageSource(config, heading, normalizedSql), totalRows);
    }

    private Long countRows(ConnectionConfig config, String sql) throws SQLException {
        try (
            Connection connection = connectionFactory.open(config);
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM (" + sql + ") q")
        ) {
            if (!resultSet.next()) {
                return Long.valueOf(0L);
            }
            Object value = resultSet.getObject(1);
            if (value instanceof Number) {
                return Long.valueOf(((Number) value).longValue());
            }
            return Long.valueOf(String.valueOf(value));
        }
    }

    private String pageSql(ConnectionConfig config, String sql, int offset, int pageSize) {
        if (config.databaseType() == DatabaseType.POSTGRESQL) {
            return "SELECT * FROM (" + sql + ") q LIMIT " + pageSize + " OFFSET " + offset;
        }
        return "SELECT * FROM (" + sql + ") q OFFSET " + offset + " ROWS FETCH NEXT " + pageSize + " ROWS ONLY";
    }

    public void exportCsv(ConnectionConfig config, String script, File outputFile) throws Exception {
        if (script == null || script.trim().isEmpty()) {
            throw new IllegalArgumentException("script es obligatorio");
        }
        List<String> statements = splitStatements(script);
        if (statements.size() != 1) {
            throw new IllegalArgumentException("export requiere exactamente un SELECT");
        }

        try (
            Connection connection = connectionFactory.open(config);
            Statement statement = connection.createStatement();
            Writer writer = new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8)
        ) {
            try (ResultSet resultSet = statement.executeQuery(statements.get(0))) {
                writeCsv(resultSet, writer);
            }
        }
    }

    private static SqlExecutionResult formatResultSet(String heading, ResultSet resultSet) throws SQLException {
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();
        List<String> headers = new ArrayList<>();
        List<List<String>> rows = new ArrayList<>();

        for (int column = 1; column <= columnCount; column++) {
            String header = metaData.getColumnLabel(column);
            headers.add(header);
        }

        while (resultSet.next()) {
            List<String> row = new ArrayList<>();
            for (int column = 1; column <= columnCount; column++) {
                String value = String.valueOf(resultSet.getObject(column));
                row.add(value);
            }
            rows.add(row);
        }

        return SqlExecutionResult.paged(heading, headers, rows);
    }

    private static void writeCsv(ResultSet resultSet, Writer writer) throws Exception {
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();

        for (int column = 1; column <= columnCount; column++) {
            if (column > 1) {
                writer.write(',');
            }
            writer.write(csv(metaData.getColumnLabel(column)));
        }
        writer.write(System.lineSeparator());

        while (resultSet.next()) {
            for (int column = 1; column <= columnCount; column++) {
                if (column > 1) {
                    writer.write(',');
                }
                Object value = resultSet.getObject(column);
                writer.write(csv(value == null ? "" : String.valueOf(value)));
            }
            writer.write(System.lineSeparator());
        }
    }

    private static String csv(String value) {
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    private static List<String> splitStatements(String script) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;

        for (int i = 0; i < script.length(); i++) {
            char ch = script.charAt(i);
            if (ch == '\'') {
                current.append(ch);
                if (inSingleQuote && i + 1 < script.length() && script.charAt(i + 1) == '\'') {
                    current.append(script.charAt(++i));
                    continue;
                }
                inSingleQuote = !inSingleQuote;
                continue;
            }
            if (ch == ';' && !inSingleQuote) {
                addStatement(statements, current);
            } else {
                current.append(ch);
            }
        }
        addStatement(statements, current);
        return statements;
    }

    private static void addStatement(List<String> statements, StringBuilder current) {
        String sql = current.toString().trim();
        if (!sql.isEmpty()) {
            statements.add(sql);
        }
        current.setLength(0);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static boolean isSelectLike(String sql) {
        String normalized = stripTrailingSemicolon(sql).trim().toLowerCase();
        return startsWithKeyword(normalized, "select") || startsWithKeyword(normalized, "with");
    }

    private static boolean startsWithKeyword(String sql, String keyword) {
        return sql.equals(keyword) || (sql.startsWith(keyword) && Character.isWhitespace(sql.charAt(keyword.length())));
    }

    private static String stripTrailingSemicolon(String sql) {
        String normalized = sql == null ? "" : sql.trim();
        while (normalized.endsWith(";")) {
            normalized = normalized.substring(0, normalized.length() - 1).trim();
        }
        return normalized;
    }

    public interface Clock {
        long currentTimeMillis();
    }

    private static final class SystemClock implements Clock {

        @Override
        public long currentTimeMillis() {
            return System.currentTimeMillis();
        }
    }

    private final class JdbcPageSource implements SqlExecutionResult.PageSource {

        private final ConnectionConfig config;
        private final String heading;
        private final String sql;
        private List<String> headers = new ArrayList<String>();

        public JdbcPageSource(ConnectionConfig config, String heading, String sql) {
            this.config = config;
            this.heading = heading;
            this.sql = sql;
        }

        @Override
        public String heading() {
            return heading;
        }

        @Override
        public List<String> headers() {
            return headers;
        }

        @Override
        public SqlExecutionResult.PageRows fetch(int pageIndex, int pageSize) {
            int offset = Math.max(0, pageIndex) * pageSize;
            try (
                Connection connection = connectionFactory.open(config);
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(pageSql(config, sql, offset, pageSize))
            ) {
                ResultSetMetaData metaData = resultSet.getMetaData();
                int columnCount = metaData.getColumnCount();
                List<String> pageHeaders = new ArrayList<String>();
                for (int column = 1; column <= columnCount; column++) {
                    pageHeaders.add(metaData.getColumnLabel(column));
                }
                List<List<String>> rows = new ArrayList<List<String>>();
                while (resultSet.next()) {
                    List<String> row = new ArrayList<String>();
                    for (int column = 1; column <= columnCount; column++) {
                        row.add(String.valueOf(resultSet.getObject(column)));
                    }
                    rows.add(row);
                }
                headers = pageHeaders;
                return new SqlExecutionResult.PageRows(rows);
            } catch (SQLException ex) {
                throw new IllegalStateException("Could not fetch result page: " + ex.getMessage(), ex);
            }
        }

        @Override
        public long currentTimeMillis() {
            return clock.currentTimeMillis();
        }
    }
}
