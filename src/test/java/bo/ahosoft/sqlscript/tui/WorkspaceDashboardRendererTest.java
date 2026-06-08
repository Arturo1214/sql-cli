package bo.ahosoft.sqlscript.tui;

import static org.junit.Assert.assertEquals;
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

public class WorkspaceDashboardRendererTest {

    @Test
    public void rendersConnectionSchemasBufferHistoryAndHints() {
        WorkspaceDashboardRenderer.DashboardState state = new WorkspaceDashboardRenderer.DashboardState(
            "reporting",
            Arrays.asList("public", "audit"),
            "select * from customers where active = true",
            Arrays.asList("select 1", "select 2"),
            "3 rows returned",
            null
        );

        String dashboard = new WorkspaceDashboardRenderer().render(state);

        assertTrue(dashboard.contains("Interactive Workspace"));
        assertTrue(dashboard.contains("Active connection: reporting"));
        assertTrue(dashboard.contains("Schemas: public, audit"));
        assertTrue(dashboard.contains("Buffer: select * from customers"));
        assertTrue(dashboard.contains("History: select 1 | select 2"));
        assertTrue(dashboard.contains("Last result: 3 rows returned"));
        assertTrue(
            dashboard.contains(
                "Commands: help, connections, use <name>, new connection, schemas, buffer, run, tables, desc, sample, history, exit"
            )
        );
    }

    @Test
    public void rendersLastErrorAndEmptyPlaceholders() {
        WorkspaceDashboardRenderer.DashboardState state = new WorkspaceDashboardRenderer.DashboardState(
            null,
            Arrays.<String>asList(),
            "",
            Arrays.<String>asList(),
            null,
            "Nothing is ready to execute"
        );

        String dashboard = new WorkspaceDashboardRenderer().render(state);

        assertTrue(dashboard.contains("Active connection: none"));
        assertTrue(dashboard.contains("Schemas: none"));
        assertTrue(dashboard.contains("Buffer: empty"));
        assertTrue(dashboard.contains("History: empty"));
        assertTrue(dashboard.contains("Last error: Nothing is ready to execute"));
    }

    @Test
    public void rendersFullWidthSplitWhenAnsiTerminalIsLarge() {
        WorkspaceDashboardRenderer.DashboardState state = new WorkspaceDashboardRenderer.DashboardState(
            "reporting",
            Arrays.asList("public", "audit"),
            "select customer_id, status from customers where active = true",
            Arrays.asList("select 1", "select 2"),
            "CUSTOMER_ID | STATUS\n1 | ACTIVE",
            null,
            Arrays.asList(
                new WorkspaceDashboardRenderer.ConnectionSummary("reporting", "postgresql", "QA", true),
                new WorkspaceDashboardRenderer.ConnectionSummary("warehouse", "oracle", "PROD", false)
            ),
            "Ready"
        );

        String dashboard = new WorkspaceDashboardRenderer()
            .render(state, new WorkspaceDashboardRenderer.TerminalCapabilities(true, new WorkspaceDashboardRenderer.TerminalSize(100, 30)));

        assertTrue(dashboard.contains("\u001B[2J"));
        assertTrue(dashboard.contains("┌"));
        assertFalse(dashboard.contains("== Interactive Workspace (Compact) =="));
        assertTrue(dashboard.contains("* [QA] reporting"));
        assertTrue(dashboard.contains("!! PROD !! [PROD]"));
        assertTrue(dashboard.contains("SQL Buffer"));
        assertTrue(dashboard.contains("select customer_id, status from customers where active = true"));
        assertTrue(dashboard.contains("Results"));
        assertTrue(dashboard.contains("CUSTOMER_ID | STATUS"));
        assertTrue(dashboard.contains("Ready | Commands:"));
        assertTrue(dashboard.contains(WorkspaceDashboardRenderer.commandHints()));
        assertRenderedFrameWidth(dashboard, 100);
    }

    @Test
    public void splitRendererUsesWideTerminalWidthForEditorResultsAndSeparators() {
        String wideSql = "select very_wide_column_name_that_used_to_be_hidden_past_old_caps from important_reporting_table";
        String wideResult = "RESULT_COLUMN_A | RESULT_COLUMN_B | RESULT_COLUMN_C | RESULT_COLUMN_D | RESULT_COLUMN_E | RESULT_COLUMN_F";
        WorkspaceDashboardRenderer.DashboardState state = new WorkspaceDashboardRenderer.DashboardState(
            "reporting",
            Collections.singletonList("public"),
            wideSql,
            Collections.<String>emptyList(),
            wideResult,
            null,
            Collections.singletonList(new WorkspaceDashboardRenderer.ConnectionSummary("reporting", "postgresql", true)),
            "Ready"
        );

        String dashboard = new WorkspaceDashboardRenderer()
            .render(state, new WorkspaceDashboardRenderer.TerminalCapabilities(true, new WorkspaceDashboardRenderer.TerminalSize(160, 36)));

        assertRenderedFrameWidth(dashboard, 160);
        assertTrue(dashboard.contains("very_wide_column_name_that_used_to_be_hidden_past_old_caps"));
        assertTrue(dashboard.contains("RESULT_COLUMN_A | RESULT_COLUMN_B | RESULT_COLUMN_C | RESULT_COLUMN_D | RESULT_COLUMN_E"));
        assertFalse(
            "wide output should not be clipped at an old 80 column cap",
            dashboard.contains("RESULT_COLUMN_A | RESULT_COLUMN_B | RESULT_COLUMN_C | RESULT_COLUMN_D | RESULT_COLUMN…")
        );
    }

    @Test
    public void rendersCompactFallbackWithoutAnsiWhenTerminalIsSmall() {
        WorkspaceDashboardRenderer.DashboardState state = new WorkspaceDashboardRenderer.DashboardState(
            "warehouse",
            Collections.singletonList("public"),
            "select * from invoices",
            Collections.singletonList("history item"),
            "2 rows returned",
            null,
            Collections.singletonList(new WorkspaceDashboardRenderer.ConnectionSummary("warehouse", "oracle", true)),
            "Compact mode"
        );

        String dashboard = new WorkspaceDashboardRenderer()
            .render(state, new WorkspaceDashboardRenderer.TerminalCapabilities(true, new WorkspaceDashboardRenderer.TerminalSize(79, 23)));

        assertFalse(dashboard.contains("\u001B[2J"));
        assertTrue(dashboard.contains("== Interactive Workspace (Compact) =="));
        assertTrue(dashboard.contains("Connections:"));
        assertTrue(dashboard.contains("* [DEV] warehouse [oracle]"));
        assertTrue(dashboard.contains("SQL Buffer:"));
        assertTrue(dashboard.contains("select * from invoices"));
        assertTrue(dashboard.contains("Results:"));
        assertTrue(dashboard.contains("2 rows returned"));
        assertTrue(dashboard.contains("Status: Compact mode"));
    }

    @Test
    public void rendersSafeOutputWithoutCursorControlWhenAnsiIsDisabled() {
        WorkspaceDashboardRenderer.DashboardState state = new WorkspaceDashboardRenderer.DashboardState(
            "warehouse",
            Collections.singletonList("public"),
            "select * from long_named_table_for_reporting",
            Collections.<String>emptyList(),
            "ok",
            null,
            Collections.singletonList(new WorkspaceDashboardRenderer.ConnectionSummary("warehouse", "oracle", true)),
            "TERM=dumb"
        );

        String dashboard = new WorkspaceDashboardRenderer()
            .render(
                state,
                new WorkspaceDashboardRenderer.TerminalCapabilities(false, new WorkspaceDashboardRenderer.TerminalSize(100, 30))
            );

        assertFalse(dashboard.contains("\u001B[2J"));
        assertTrue(dashboard.contains("== Interactive Workspace (Compact) =="));
        assertTrue(dashboard.contains("TERM=dumb"));
        assertTrue(dashboard.contains("[DEV] warehouse [oracle]"));
    }

    @Test
    public void compactFallbackKeepsLongContentReadableWithoutAnsi() {
        WorkspaceDashboardRenderer.DashboardState state = new WorkspaceDashboardRenderer.DashboardState(
            "very-long-reporting-connection-name",
            Collections.singletonList("public"),
            "select this_is_a_very_long_column_name from this_is_a_very_long_table_name where status = 'ACTIVE'",
            Collections.<String>emptyList(),
            "this result text is intentionally long enough to be clipped inside the results pane",
            null,
            Collections.singletonList(
                new WorkspaceDashboardRenderer.ConnectionSummary("very-long-reporting-connection-name", "postgresql", true)
            ),
            "Ready"
        );

        String dashboard = new WorkspaceDashboardRenderer()
            .render(state, new WorkspaceDashboardRenderer.TerminalCapabilities(true, new WorkspaceDashboardRenderer.TerminalSize(80, 24)));

        assertTrue(dashboard.contains("\u001B[2J"));
        assertFalse(dashboard.contains("== Interactive Workspace (Compact) =="));
        assertTrue(dashboard.contains("select this_is_a_very_long_column_name"));
        assertTrue(dashboard.contains("this result text is intentionally long enough"));
        assertRenderedFrameWidth(dashboard, 80);
    }

    @Test
    public void compactFallbackPreservesSqlConsoleRendererTableInsideResultsPane() {
        String table = SqlConsoleRenderer.formatRows(
            Arrays.asList("ID", "STATUS"),
            Collections.singletonList(Arrays.asList("42", "ACTIVE"))
        );
        WorkspaceDashboardRenderer.DashboardState state = new WorkspaceDashboardRenderer.DashboardState(
            "reporting",
            Collections.singletonList("public"),
            "select id, status from customers",
            Collections.<String>emptyList(),
            table,
            null,
            Collections.singletonList(new WorkspaceDashboardRenderer.ConnectionSummary("reporting", "postgresql", true)),
            "Query completed"
        );

        String dashboard = new WorkspaceDashboardRenderer()
            .render(state, new WorkspaceDashboardRenderer.TerminalCapabilities(true, new WorkspaceDashboardRenderer.TerminalSize(100, 30)));

        assertTrue(dashboard.contains("\u001B[2J"));
        assertTrue(dashboard.contains("Results"));
        assertTrue(dashboard.contains("| ID | STATUS |"));
        assertTrue(dashboard.contains("| 42 | ACTIVE |"));
        assertTrue(dashboard.contains("Total rows: 1"));
        assertRenderedFrameWidth(dashboard, 100);
    }

    private static void assertRenderedFrameWidth(String dashboard, int expectedWidth) {
        for (String line : dashboard.split("\\R")) {
            if (line.startsWith("┌") || line.startsWith("│")) {
                assertEquals("framed line should use terminal width: " + line, expectedWidth, line.length());
            }
        }
    }
}
