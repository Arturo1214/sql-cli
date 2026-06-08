package bo.ahosoft.sqlscript.sql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;
import java.util.List;
import org.junit.Test;

public class SqlDiagnosticsTest {

    @Test
    public void reportsUnmatchedQuotesAndParenthesesWithoutChangingSql() {
        String sql = "select (name from users where code = 'A";

        List<SqlDiagnostic> diagnostics = SqlDiagnostics.analyze(sql, SqlDialect.GENERIC);

        assertEquals(sql, SqlDiagnostics.normalizeForExecution(sql));
        assertContains(diagnostics, "Unmatched string quote");
        assertContains(diagnostics, "Unmatched opening parenthesis");
    }

    @Test
    public void reportsMissingSelectListBeforeFrom() {
        List<SqlDiagnostic> diagnostics = SqlDiagnostics.analyze("select from users", SqlDialect.GENERIC);

        assertContains(diagnostics, "SELECT is missing a column list before FROM");
    }

    @Test
    public void warnsForOracleConstantSelectWithoutFromButNotPostgresql() {
        List<SqlDiagnostic> oracleDiagnostics = SqlDiagnostics.analyze("select 1", SqlDialect.ORACLE);
        List<SqlDiagnostic> postgresDiagnostics = SqlDiagnostics.analyze("select 1", SqlDialect.POSTGRESQL);

        assertContains(oracleDiagnostics, "Oracle constant SELECT may need FROM dual");
        assertDoesNotContain(postgresDiagnostics, "FROM dual");
    }

    @Test
    public void suggestsEnglishMetadataCommandsForCloseVariants() {
        assertContains(SqlDiagnostics.analyze("table", SqlDialect.GENERIC), "Did you mean tables?");
        assertContains(SqlDiagnostics.analyze("desc customers", SqlDialect.GENERIC), "Did you mean describe <table>?");
        assertContains(SqlDiagnostics.analyze("index customers", SqlDialect.GENERIC), "Did you mean indexes <table>?");
    }

    @Test
    public void acceptsTrailingSemicolonWithoutRequestingRewrite() {
        List<SqlDiagnostic> diagnostics = SqlDiagnostics.analyze("select * from users;", SqlDialect.POSTGRESQL);

        assertDoesNotContain(diagnostics, "semicolon");
        assertEquals("select * from users;", SqlDiagnostics.normalizeForExecution("select * from users;"));
    }

    private static void assertContains(List<SqlDiagnostic> diagnostics, String expected) {
        for (SqlDiagnostic diagnostic : diagnostics) {
            if (diagnostic.message().contains(expected)) {
                return;
            }
        }
        assertTrue("Expected diagnostic containing: " + expected + " but got " + messages(diagnostics), false);
    }

    private static void assertDoesNotContain(List<SqlDiagnostic> diagnostics, String unexpected) {
        for (SqlDiagnostic diagnostic : diagnostics) {
            if (diagnostic.message().contains(unexpected)) {
                assertTrue("Unexpected diagnostic containing: " + unexpected + " in " + messages(diagnostics), false);
            }
        }
    }

    private static String messages(List<SqlDiagnostic> diagnostics) {
        StringBuilder builder = new StringBuilder();
        for (SqlDiagnostic diagnostic : diagnostics) {
            if (builder.length() > 0) {
                builder.append(" | ");
            }
            builder.append(diagnostic.message());
        }
        return builder.toString();
    }
}
